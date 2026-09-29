package com.qrcode.scanner.launcher.helpers;

import android.content.Context;

import com.qrcode.scanner.launcher.common.AppUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LocaleHelper {
    public interface LanguageChangeListener {
        void onLanguageChanged();
    }

    private static final List<LanguageChangeListener> LANGUAGE_CHANGE_LISTENERS = new CopyOnWriteArrayList<>();

    private LocaleHelper() {
    }

    public static void registerLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null) {
            LANGUAGE_CHANGE_LISTENERS.add(listener);
        }
    }

    public static void unregisterLanguageChangeListener(LanguageChangeListener listener) {
        LANGUAGE_CHANGE_LISTENERS.remove(listener);
    }

    public static void syncResources(Context context) {
        if (context == null) {
            return;
        }
        AppUtils.restoreSavedLanguage(context.getApplicationContext());
        notifyLanguageChanged();
    }

    private static void notifyLanguageChanged() {
        for (LanguageChangeListener listener : LANGUAGE_CHANGE_LISTENERS) {
            listener.onLanguageChanged();
        }
    }

    public static Context wrap(Context context) {
        AppUtils.restoreSavedLanguage(context);
        return context;
    }
}
