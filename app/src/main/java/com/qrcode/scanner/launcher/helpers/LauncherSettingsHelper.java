package com.qrcode.scanner.launcher.helpers;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.qrcode.scanner.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LauncherSettingsHelper {
    public interface ChangeListener {
        void onLauncherSettingsChanged(int changeMask);
    }

    public static final int CHANGE_ICON_SIZE = 1;
    public static final int CHANGE_LABEL_SIZE = 2;
    public static final int CHANGE_LABEL_VISIBILITY = 4;
    public static final int CHANGE_SERIALIZE = 8;
    public static final int CHANGE_DISPLAY = CHANGE_ICON_SIZE | CHANGE_LABEL_SIZE | CHANGE_LABEL_VISIBILITY;

    private static final List<ChangeListener> CHANGE_LISTENERS = new CopyOnWriteArrayList<>();
    public static final String SERIALIZE_NAME_ASC = "a";
    public static final String SERIALIZE_NAME_DESC = "d";
    public static final String SERIALIZE_INSTALLED_DATE = "i";
    public static final String DEFAULT_APP_SERIALIZE = SERIALIZE_NAME_ASC;

    public static final boolean DEFAULT_LABEL_VISIBILITY = true;

    public static final int MIN_APP_ICON_SIZE = 40;
    public static final int MAX_APP_ICON_SIZE = 80;
    public static final int DEFAULT_APP_ICON_SIZE = 60;

    public static final int MIN_APP_LABEL_SIZE = 10;
    public static final int MAX_APP_LABEL_SIZE = 16;
    public static final int DEFAULT_APP_LABEL_SIZE = 12;

    private static final String PREFS_NAME = "launcher_settings";
    private static final String KEY_LABEL_VISIBILITY = "labelVisibility";
    private static final String KEY_APP_ICON_SIZE = "appIconSize";
    private static final String KEY_APP_LABEL_SIZE = "appLabelSize";
    private static final String KEY_APP_SERIALIZE = "appSerialize";
    private static final String KEY_RIGHT_SWIPE_TUTORIAL_SHOWN = "right_swipe_tutorial_shown";

    private LauncherSettingsHelper() {
    }

    public static void registerChangeListener(ChangeListener listener) {
        if (listener != null) {
            CHANGE_LISTENERS.add(listener);
        }
    }

    public static void unregisterChangeListener(ChangeListener listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    private static void notifyChanged(int changeMask) {
        for (ChangeListener listener : CHANGE_LISTENERS) {
            listener.onLauncherSettingsChanged(changeMask);
        }
    }

    public static String getAppSerialize(Context context) {
        String serialize = getPrefs(context).getString(KEY_APP_SERIALIZE, DEFAULT_APP_SERIALIZE);
        return isValidSerialize(serialize) ? serialize : DEFAULT_APP_SERIALIZE;
    }

    public static void setAppSerialize(Context context, String value) {
        if (!isValidSerialize(value)) {
            return;
        }
        if (value.equals(getAppSerialize(context))) {
            return;
        }
        getPrefs(context).edit().putString(KEY_APP_SERIALIZE, value).apply();
        notifyChanged(CHANGE_SERIALIZE);
    }

    public static int getSerializeLabelResId(String serialize) {
        if (SERIALIZE_NAME_DESC.equals(serialize)) {
            return R.string.by_name_desc_z_to_a;
        }
        if (SERIALIZE_INSTALLED_DATE.equals(serialize)) {
            return R.string.by_installed_date;
        }
        return R.string.by_name_asc_a_to_z;
    }

    private static boolean isValidSerialize(String value) {
        return SERIALIZE_NAME_ASC.equals(value) || SERIALIZE_NAME_DESC.equals(value) || SERIALIZE_INSTALLED_DATE.equals(value);
    }

    public static boolean getLabelVisibility(Context context) {
        return getPrefs(context).getBoolean(KEY_LABEL_VISIBILITY, DEFAULT_LABEL_VISIBILITY);
    }

    public static void setLabelVisibility(Context context, boolean value) {
        if (getLabelVisibility(context) == value) {
            return;
        }
        getPrefs(context).edit().putBoolean(KEY_LABEL_VISIBILITY, value).apply();
        notifyChanged(CHANGE_LABEL_VISIBILITY);
    }

    public static int getAppIconSize(Context context) {
        int size = getPrefs(context).getInt(KEY_APP_ICON_SIZE, DEFAULT_APP_ICON_SIZE);
        return clampAppIconSize(size);
    }

    public static void setAppIconSize(Context context, int value) {
        int clamped = clampAppIconSize(value);
        if (getAppIconSize(context) == clamped) {
            return;
        }
        getPrefs(context).edit().putInt(KEY_APP_ICON_SIZE, clamped).apply();
        notifyChanged(CHANGE_ICON_SIZE);
    }

    public static int clampAppIconSize(int size) {
        return Math.max(MIN_APP_ICON_SIZE, Math.min(MAX_APP_ICON_SIZE, size));
    }

    public static int getAppLabelSize(Context context) {
        int size = getPrefs(context).getInt(KEY_APP_LABEL_SIZE, DEFAULT_APP_LABEL_SIZE);
        return clampAppLabelSize(size);
    }

    public static void setAppLabelSize(Context context, int value) {
        int clamped = clampAppLabelSize(value);
        if (getAppLabelSize(context) == clamped) {
            return;
        }
        getPrefs(context).edit().putInt(KEY_APP_LABEL_SIZE, clamped).apply();
        notifyChanged(CHANGE_LABEL_SIZE);
    }

    public static int clampAppLabelSize(int size) {
        return Math.max(MIN_APP_LABEL_SIZE, Math.min(MAX_APP_LABEL_SIZE, size));
    }

    public static boolean isRightSwipeTutorialShown(Context context) {
        return getPrefs(context).getBoolean(KEY_RIGHT_SWIPE_TUTORIAL_SHOWN, false);
    }

    public static void setRightSwipeTutorialShown(Context context, boolean shown) {
        getPrefs(context).edit().putBoolean(KEY_RIGHT_SWIPE_TUTORIAL_SHOWN, shown).apply();
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
}
