package com.qrcode.scanner.launcher.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qrcode.scanner.launcher.common.IPAddressHelper;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Non-blocking city/state/country match used to choose the active quiz link list.
 * Startup continues immediately. A failed lookup keeps the normal quiz link list.
 */
public final class AppProxyLookup {
    private static final AtomicInteger LOOKUP_TOKEN = new AtomicInteger();

    private AppProxyLookup() {
    }

    public static void refreshActiveQuizLinks() {
        if (!RemoteConfigValues.isAppProxyCheckIp()) {
            RemoteConfigValues.setActiveQuizLinkList(RemoteConfigValues.getQuizLinkList());
            return;
        }

        RemoteConfigValues.setActiveQuizLinkList(RemoteConfigValues.getQuizLinkList());
        String url = RemoteConfigValues.getAppProxyIpCheckerUrl();
        if (url.isEmpty()) {
            return;
        }

        final int lookupToken = LOOKUP_TOKEN.incrementAndGet();
        IPAddressHelper.getLocationInfo(url, new IPAddressHelper.LocationCallback() {
            @Override
            public void onResponse(@NonNull IPAddressHelper.LocationInfo locationInfo) {
                if (lookupToken != LOOKUP_TOKEN.get()) {
                    return;
                }
                if (matches(locationInfo)) {
                    List<String> exclude = RemoteConfigValues.getQuizLinkExcludeList();
                    RemoteConfigValues.setActiveQuizLinkList(exclude.isEmpty() ? RemoteConfigValues.getQuizLinkList() : exclude);
                } else {
                    RemoteConfigValues.setActiveQuizLinkList(RemoteConfigValues.getQuizLinkList());
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (lookupToken != LOOKUP_TOKEN.get()) {
                    return;
                }
                RemoteConfigValues.setActiveQuizLinkList(RemoteConfigValues.getQuizLinkList());
            }
        });
    }

    private static boolean matches(@NonNull IPAddressHelper.LocationInfo locationInfo) {
        return contains(RemoteConfigValues.getAppProxyListCity(), locationInfo.city)
                || contains(RemoteConfigValues.getAppProxyListState(), locationInfo.state)
                || contains(RemoteConfigValues.getAppProxyListCountry(), locationInfo.country);
    }

    private static boolean contains(@NonNull List<String> configuredValues, @Nullable String detectedValue) {
        if (detectedValue == null || detectedValue.trim().isEmpty() || configuredValues.isEmpty()) {
            return false;
        }
        String normalizedDetectedValue = detectedValue.trim().toLowerCase(Locale.US);
        for (String configuredValue : configuredValues) {
            if (configuredValue != null && !configuredValue.trim().isEmpty() && configuredValue.trim().toLowerCase(Locale.US).equals(normalizedDetectedValue)) {
                return true;
            }
        }
        return false;
    }
}
