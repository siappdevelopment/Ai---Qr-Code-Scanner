package com.qrcode.scanner.launcher.activities;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

/** App install / uninstall screen, driven by the "install_uninstall_screen" config. */
public class InstallUninstallScreenActivity extends EventPromptActivity {

    @Override
    protected int layoutRes() {
        return R.layout.activity_install_uninstall_screen;
    }

    @Override
    protected String screenKey() {
        return RemoteConfigValues.EVENT_SCREEN_INSTALL_UNINSTALL;
    }

    @Override
    protected String kindFrom(@Nullable Intent intent) {
        String kind = intent == null ? null : intent.getStringExtra(EXTRA_KIND);
        return KIND_INSTALL.equals(kind) ? KIND_INSTALL : KIND_UNINSTALL;
    }

    @Override
    protected void bindKind(String kind) {
        if (KIND_INSTALL.equals(kind)) {
            showContent(0xFFEAF1FB, 0xFF0063E5, 0xFFDBEAFE, 0xFF1D4ED8,
                    getString(R.string.event_install_title),
                    getString(R.string.event_install_success),
                    R.string.event_install_sub, R.string.event_install_body, R.string.event_install_chip,
                    getString(R.string.event_fast),
                    R.drawable.ic_event_hero_install, R.drawable.ic_event_check, R.drawable.ic_event_check);
            return;
        }
        showContent(0xFFEAF1FB, 0xFF0063E5, 0xFFDBEAFE, 0xFF1D4ED8,
                getString(R.string.event_uninstall_title),
                getString(R.string.event_uninstall_success),
                R.string.event_uninstall_sub, R.string.event_uninstall_body, R.string.event_uninstall_chip,
                getString(R.string.event_fast),
                R.drawable.ic_event_hero_uninstall, R.drawable.ic_event_check, R.drawable.ic_event_check);
    }
}
