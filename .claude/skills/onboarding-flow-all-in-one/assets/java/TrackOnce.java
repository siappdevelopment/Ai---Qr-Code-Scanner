package com.qrcode.scanner.launcher.common;

import android.content.Context;

public final class TrackOnce {
    private TrackOnce() {
    }

    public static void trackScreenOnce(Context context, String eventName) {
        AppUtils.trackScreenOnce(context, eventName);
    }
}
