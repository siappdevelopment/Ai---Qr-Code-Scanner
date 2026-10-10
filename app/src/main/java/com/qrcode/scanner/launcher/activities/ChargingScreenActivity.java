package com.qrcode.scanner.launcher.activities;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/** Charger plug-in / plug-out screen, driven by the "charging_screen" config. */
public class ChargingScreenActivity extends EventPromptActivity {

    @Override
    protected int layoutRes() {
        return R.layout.activity_charging_screen;
    }

    @Override
    protected String screenKey() {
        return RemoteConfigValues.EVENT_SCREEN_CHARGING;
    }

    @Override
    protected String kindFrom(@Nullable Intent intent) {
        String kind = intent == null ? null : intent.getStringExtra(EXTRA_KIND);
        return KIND_CHARGE_OUT.equals(kind) ? KIND_CHARGE_OUT : KIND_CHARGE_IN;
    }

    /** A plug in / plug out event while this screen is open only updates the content; ads are not requested again. */
    @Override
    protected boolean reloadAdsOnNewIntent() {
        return false;
    }

    @Override
    protected void bindKind(String kind) {
        int level = batteryPercent();
        if (KIND_CHARGE_OUT.equals(kind)) {
            showContent(0xFFFFF4E8, 0xFFEA580C, 0xFFFFEDD5, 0xFF9A3412,
                    getString(R.string.event_charge_out_title),
                    getString(R.string.event_charge_out_success, level),
                    R.string.event_charge_out_sub, R.string.event_charge_out_body, R.string.event_charge_out_chip,
                    level + "%",
                    R.drawable.ic_event_hero_charge_out, R.drawable.ic_event_bolt, R.drawable.ic_event_bolt);
            return;
        }
        showContent(0xFFE7F8EF, 0xFF16A34A, 0xFFDDF6E8, 0xFF166534,
                getString(R.string.event_charge_in_title),
                getString(R.string.event_charge_in_success, level),
                R.string.event_charge_in_sub, R.string.event_charge_in_body, R.string.event_charge_in_chip,
                level + "%",
                R.drawable.ic_event_hero_charge_in, R.drawable.ic_event_check, R.drawable.ic_event_bolt);
    }
}
