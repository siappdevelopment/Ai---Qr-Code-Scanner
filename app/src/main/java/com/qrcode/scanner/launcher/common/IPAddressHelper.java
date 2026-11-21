package com.qrcode.scanner.launcher.common;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * IP geolocation request. The URL always comes from Remote Config (App_Proxy_Ip_Checker_Url);
 * no key or URL is stored in source. Understands ip-api, ipwho.is, ipapi.co and ipinfo answers.
 * The callback is always delivered on the main thread.
 */
public class IPAddressHelper {
    private static final String TAG = "IPAddressHelper";
    private static final int TIMEOUT_MILLIS = 10_000;
    private static final int MAX_BODY_CHARS = 64 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ip-location");
        thread.setDaemon(true);
        return thread;
    });

    public static final class LocationInfo {
        public final String city;
        public final String state;
        public final String country;
        public final String countryCode;

        public LocationInfo(String city, String state, String country) {
            this(city, state, country, "");
        }

        public LocationInfo(String city, String state, String country, String countryCode) {
            this.city = clean(city);
            this.state = clean(state);
            this.country = clean(country);
            this.countryCode = clean(countryCode);
        }

        public boolean hasPlace() {
            return !country.isEmpty() || !countryCode.isEmpty() || !state.isEmpty();
        }
    }

    public interface LocationCallback {
        void onResponse(@NonNull LocationInfo locationInfo);

        void onFailure(Exception e);
    }

    /**
     * Requests the place of this device from the given IP-location URL.
     * A blank URL fails at once (there is no built-in default URL).
     */
    public static void getLocationInfo(@Nullable String apiUrl, @NonNull LocationCallback callback) {
        final String requestUrl = apiUrl == null ? "" : apiUrl.trim();
        Handler handler = new Handler(Looper.getMainLooper());
        if (requestUrl.isEmpty()) {
            handler.post(() -> callback.onFailure(new Exception("IP checker URL is blank")));
            return;
        }
        EXECUTOR.execute(() -> {
            LocationInfo info = null;
            Exception failure = null;
            try {
                info = fetch(requestUrl);
            } catch (Exception e) {
                failure = e;
            }
            final LocationInfo result = info;
            final Exception error = failure;
            handler.post(() -> {
                if (result != null) {
                    callback.onResponse(result);
                } else {
                    callback.onFailure(error == null ? new Exception("IP lookup failed") : error);
                }
            });
        });
    }

    @NonNull
    private static LocationInfo fetch(@NonNull String requestUrl) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(requestUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String body = readBody(connection.getErrorStream());
                Log.w(TAG, "IP lookup HTTP " + code + ": " + (body.length() > 300 ? body.substring(0, 300) : body));
                throw new Exception("HTTP error code: " + code);
            }
            return parse(readBody(connection.getInputStream()));
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Maps the answer of any common IP service. Throws when the service reports a failure
     * or when the answer holds no place at all.
     */
    @NonNull
    static LocationInfo parse(@NonNull String body) throws Exception {
        JSONObject json = new JSONObject(body);
        if (json.has("status") && !"success".equalsIgnoreCase(json.optString("status"))) {
            throw new Exception("Location lookup failed: " + json.optString("message", json.optString("status")));
        }
        if (json.has("success") && !json.optBoolean("success", true)) {
            throw new Exception("Location lookup failed: " + json.optString("message", "success=false"));
        }
        if (json.has("error") && !json.isNull("error") && !Boolean.FALSE.equals(json.opt("error"))) {
            throw new Exception("Location lookup failed: " + json.opt("error"));
        }
        String countryField = text(json, "country");
        String code = firstNonEmpty(text(json, "countryCode"), text(json, "country_code"));
        if (code.isEmpty() && countryField.length() == 2) {
            code = countryField;
        }
        String name = firstNonEmpty(text(json, "country_name"), countryField.length() == 2 ? "" : countryField);
        if (name.isEmpty() && code.length() == 2) {
            name = englishCountryName(code);
        }
        String region = firstNonEmpty(text(json, "regionName"), text(json, "region_name"), text(json, "region"));
        String city = text(json, "city");
        LocationInfo info = new LocationInfo(city, region, name, code);
        if (!info.hasPlace()) {
            throw new Exception("IP answer has no place");
        }
        return info;
    }

    @NonNull
    private static String englishCountryName(@NonNull String isoCode) {
        try {
            String name = new Locale.Builder().setRegion(isoCode).build().getDisplayCountry(Locale.ENGLISH);
            return name == null || name.equalsIgnoreCase(isoCode) ? "" : name;
        } catch (Exception ignored) {
            return "";
        }
    }

    @NonNull
    private static String readBody(@Nullable InputStream stream) {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1 && builder.length() < MAX_BODY_CHARS) {
                builder.append(buffer, 0, read);
            }
        } catch (Exception ignored) {
        }
        return builder.toString();
    }

    @NonNull
    private static String text(@NonNull JSONObject json, @NonNull String key) {
        if (!json.has(key) || json.isNull(key)) {
            return "";
        }
        return clean(json.optString(key, ""));
    }

    @NonNull
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @NonNull
    private static String clean(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}
