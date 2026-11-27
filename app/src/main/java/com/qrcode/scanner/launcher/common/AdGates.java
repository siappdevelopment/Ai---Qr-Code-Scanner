package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Common ads location rule.
 * <p>
 * Remote Config keys (profile root): Ads_Show_countryList (include) and Ads_show_List_Exclude (exclude).
 * Entries are an English country name, a 2-letter ISO code, a state or a city (case-insensitive).
 * <ul>
 *   <li>Both lists empty: allowed everywhere, the IP API is never called.</li>
 *   <li>A place of the device is in the exclude list: blocked (exclude always wins).</li>
 *   <li>Include list not empty and no place of the device is in it: blocked.</li>
 * </ul>
 * Places of the device = device country (network / SIM / phone locale, ISO code and English name; works
 * without the API) + IP location (country, ISO code, state, city; from App_Proxy_Ip_Checker_Url, the last
 * answer is saved and reused after a restart or when the API fails).
 */
public final class AdGates {
    private static final String TAG = "AdGates";
    private static final String PREFS = "ads_location_preferences";
    private static final String KEY_IP_INFO = "ip_info";
    private static final String KEY_INCLUDE = "include_list";
    private static final String KEY_EXCLUDE = "exclude_list";
    private static final String KEY_URL = "ip_checker_url";

    /** Longest wait of the Splash ad slot for the first IP answer. */
    public static final long SPLASH_IP_WAIT_MILLIS = 2_000L;
    private static final long WAIT_POLL_MILLIS = 100L;
    /** A failed lookup is retried by the next trigger, but not more often than this. */
    private static final long RETRY_GAP_MILLIS = 15_000L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    @Nullable
    private static volatile Context appContext;
    private static volatile List<String> includeLocations = Collections.emptyList();
    private static volatile List<String> excludeLocations = Collections.emptyList();
    private static volatile String savedIpCheckerUrl = "";
    @Nullable
    private static volatile IPAddressHelper.LocationInfo ipInfo;
    private static boolean ipLookupStarted;
    private static long lastFailureAt;
    @Nullable
    private static String lastLoggedState;

    private AdGates() {
    }

    // ---------------------------------------------------------------- setup

    /** Loads the saved lists and the saved IP location. Safe to call many times. */
    public static void init(@Nullable Context context) {
        if (appContext != null || context == null) {
            return;
        }
        initLocked(context);
    }

    private static synchronized void initLocked(@NonNull Context context) {
        if (appContext != null) {
            return;
        }
        Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
        appContext = app;
        try {
            SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            includeLocations = decodeList(prefs.getString(KEY_INCLUDE, ""));
            excludeLocations = decodeList(prefs.getString(KEY_EXCLUDE, ""));
            savedIpCheckerUrl = prefs.getString(KEY_URL, "");
            ipInfo = readSavedIpInfo(prefs);
        } catch (Exception e) {
            Log.w(TAG, "Could not read the saved ads location", e);
        }
    }

    /** Called after every Remote Config apply with the two common lists (empty when the keys are missing). */
    public static void onConfigApplied(@Nullable Context context, @Nullable List<String> include, @Nullable List<String> exclude) {
        init(context);
        Context app = appContext;
        List<String> newInclude = normalize(include);
        List<String> newExclude = normalize(exclude);
        includeLocations = newInclude;
        excludeLocations = newExclude;
        String liveUrl = RemoteConfigValues.getAppProxyIpCheckerUrl();
        if (!liveUrl.isEmpty()) {
            savedIpCheckerUrl = liveUrl;
        }
        if (app != null) {
            try {
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_URL, savedIpCheckerUrl)
                        .putString(KEY_INCLUDE, encodeList(newInclude))
                        .putString(KEY_EXCLUDE, encodeList(newExclude))
                        .apply();
            } catch (Exception e) {
                Log.w(TAG, "Could not save the ads location lists", e);
            }
        }
        requestIpCountryIfNeeded(app);
    }

    // ---------------------------------------------------------------- the rule

    /** The common rule applied to the two common Remote Config lists. */
    public static boolean adsLocationAllowed() {
        return adsLocationAllowed(appContext);
    }

    public static boolean adsLocationAllowed(@Nullable Context context) {
        init(context);
        boolean allowed = isLocationAllowed(context, includeLocations, excludeLocations);
        logIfChanged(context, allowed);
        return allowed;
    }

    /** Rule: both empty = allowed; any exclude match = blocked; include set without a match = blocked. */
    public static boolean isLocationAllowed(@Nullable Context context, @Nullable List<String> include, @Nullable List<String> exclude) {
        List<String> inc = normalize(include);
        List<String> exc = normalize(exclude);
        if (inc.isEmpty() && exc.isEmpty()) {
            return true;
        }
        Set<String> places = devicePlaces(context);
        if (anyMatch(places, exc)) {
            return false;
        }
        return inc.isEmpty() || anyMatch(places, inc);
    }

    private static boolean anyMatch(@NonNull Set<String> places, @NonNull List<String> list) {
        for (String entry : list) {
            if (places.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- places

    /** Lower-case places of this device: device country (code + name) and the known IP location. */
    @NonNull
    public static Set<String> devicePlaces(@Nullable Context context) {
        Set<String> places = new HashSet<>();
        String iso = deviceCountryIso(context);
        if (!iso.isEmpty()) {
            places.add(iso);
            String name = englishCountryName(iso);
            if (!name.isEmpty()) {
                places.add(name);
            }
        }
        IPAddressHelper.LocationInfo info = ipInfo;
        if (info != null) {
            addPlace(places, info.country);
            addPlace(places, info.countryCode);
            addPlace(places, info.state);
            addPlace(places, info.city);
        }
        return places;
    }

    /** Lower-case 2-letter device country: network, then SIM, then phone locale. Empty when unknown. */
    @NonNull
    public static String deviceCountryIso(@Nullable Context context) {
        String iso = "";
        try {
            if (context != null) {
                TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephony != null) {
                    iso = clean(telephony.getNetworkCountryIso());
                    if (iso.isEmpty()) {
                        iso = clean(telephony.getSimCountryIso());
                    }
                }
            }
        } catch (Exception ignored) {
            iso = "";
        }
        if (iso.isEmpty()) {
            iso = clean(Locale.getDefault().getCountry());
        }
        return iso;
    }

    @NonNull
    private static String englishCountryName(@NonNull String iso) {
        try {
            String name = new Locale.Builder().setRegion(iso).build().getDisplayCountry(Locale.ENGLISH);
            return name == null || name.equalsIgnoreCase(iso) ? "" : name.trim().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void addPlace(@NonNull Set<String> places, @Nullable String value) {
        String cleaned = clean(value);
        if (!cleaned.isEmpty()) {
            places.add(cleaned);
        }
    }

    // ---------------------------------------------------------------- IP lookup

    /** Live URL from Remote Config, else the last saved one (so the lookup can start before the first fetch ends). */
    @NonNull
    private static String ipCheckerUrl() {
        String live = RemoteConfigValues.getAppProxyIpCheckerUrl();
        return live.isEmpty() ? savedIpCheckerUrl : live;
    }

    /** True when the saved or fresh IP location is known. */
    public static boolean hasIpLocation() {
        return ipInfo != null;
    }

    /** True when any list that needs state / city / IP country is set. */
    public static boolean usesLocationLists() {
        return !includeLocations.isEmpty() || !excludeLocations.isEmpty();
    }

    /**
     * Starts the IP lookup when it is needed: a list is set, the URL is not
     * blank and no lookup is running or finished in this run. A failure allows the next trigger to retry.
     */
    public static void requestIpCountryIfNeeded(@Nullable Context context) {
        init(context);
        final Context app = appContext;
        if (app == null) {
            return;
        }
        if (!usesLocationLists()) {
            return;
        }
        final String url = ipCheckerUrl();
        if (url.isEmpty()) {
            return;
        }
        synchronized (AdGates.class) {
            if (ipLookupStarted) {
                return;
            }
            if (lastFailureAt != 0L && System.currentTimeMillis() - lastFailureAt < RETRY_GAP_MILLIS) {
                return;
            }
            ipLookupStarted = true;
        }
        IPAddressHelper.getLocationInfo(url, new IPAddressHelper.LocationCallback() {
            @Override
            public void onResponse(@NonNull IPAddressHelper.LocationInfo locationInfo) {
                ipInfo = locationInfo;
                saveIpInfo(app, locationInfo);
                if (!locationInfo.country.isEmpty()) {
                    AdPlacement.setIpCountryName(app, locationInfo.country);
                }
                synchronized (AdGates.class) {
                    lastFailureAt = 0L;
                }
                Log.i(TAG, "Ads location: IP lookup ok, country=" + locationInfo.country + ", code=" + locationInfo.countryCode
                        + ", state=" + locationInfo.state + ", city=" + locationInfo.city);
            }

            @Override
            public void onFailure(Exception e) {
                synchronized (AdGates.class) {
                    ipLookupStarted = false;
                    lastFailureAt = System.currentTimeMillis();
                }
                Log.w(TAG, "Ads location: the IP lookup failed (check App_Proxy_Ip_Checker_Url and its key); "
                        + "state / city are unknown: " + e.getMessage());
            }
        });
    }

    /**
     * Runs {@code onDone} on the main thread: at once when no wait is needed (no list, no URL, or a location is
     * already known, also a saved one), otherwise when the IP answer arrives or after {@code maxMillis}.
     * Runs exactly once.
     */
    public static void awaitIpLocation(@Nullable Context context, long maxMillis, @NonNull Runnable onDone) {
        init(context);
        requestIpCountryIfNeeded(context);
        if (!usesLocationLists() || ipCheckerUrl().isEmpty() || hasIpLocation() || maxMillis <= 0L) {
            runOnMain(onDone);
            return;
        }
        final long deadline = System.currentTimeMillis() + maxMillis;
        final boolean[] finished = {false};
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (finished[0]) {
                    return;
                }
                if (hasIpLocation() || System.currentTimeMillis() >= deadline) {
                    finished[0] = true;
                    onDone.run();
                    return;
                }
                MAIN.postDelayed(this, WAIT_POLL_MILLIS);
            }
        };
        MAIN.postDelayed(poll, WAIT_POLL_MILLIS);
    }

    private static void runOnMain(@NonNull Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.post(runnable);
        }
    }

    // ---------------------------------------------------------------- saved IP location

    private static void saveIpInfo(@NonNull Context context, @NonNull IPAddressHelper.LocationInfo info) {
        try {
            JSONObject json = new JSONObject();
            json.put("country", info.country);
            json.put("countryCode", info.countryCode);
            json.put("city", info.city);
            json.put("region", info.state);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_IP_INFO, json.toString()).apply();
        } catch (Exception e) {
            Log.w(TAG, "Could not save the IP location", e);
        }
    }

    @Nullable
    private static IPAddressHelper.LocationInfo readSavedIpInfo(@NonNull SharedPreferences prefs) {
        String raw = prefs.getString(KEY_IP_INFO, "");
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(raw);
            IPAddressHelper.LocationInfo info = new IPAddressHelper.LocationInfo(
                    json.optString("city", ""), json.optString("region", ""),
                    json.optString("country", ""), json.optString("countryCode", ""));
            return info.hasPlace() ? info : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    // ---------------------------------------------------------------- list helpers

    @NonNull
    private static List<String> normalize(@Nullable List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isEmpty() && !result.contains(cleaned)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @NonNull
    private static String encodeList(@NonNull List<String> values) {
        return new org.json.JSONArray(values).toString();
    }

    @NonNull
    private static List<String> decodeList(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(raw);
            List<String> values = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                values.add(array.optString(i, ""));
            }
            return normalize(values);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static void logIfChanged(@Nullable Context context, boolean allowed) {
        if (includeLocations.isEmpty() && excludeLocations.isEmpty()) {
            return;
        }
        Set<String> places = devicePlaces(context);
        String state = allowed + "|" + places + "|" + includeLocations + "|" + excludeLocations;
        synchronized (AdGates.class) {
            if (state.equals(lastLoggedState)) {
                return;
            }
            lastLoggedState = state;
        }
        Log.i(TAG, "Ads location: allowed=" + allowed + ", places=" + places
                + ", include=" + includeLocations + ", exclude=" + excludeLocations);
    }
}
