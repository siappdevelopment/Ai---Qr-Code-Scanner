package com.qrcode.scanner.launcher.services;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.common.ADSNativeFullDisplay;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.CallEndLaunchHelper;
import com.qrcode.scanner.launcher.common.ProcessAppOpen;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    }

    @Override
    public void handleIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Map<String, String> data = null;
        RemoteMessage remoteMessage = null;
        if (intent != null && intent.getExtras() != null) {
            remoteMessage = new RemoteMessage(intent.getExtras());
            data = remoteMessage.getData();
        }

        if (remoteMessage != null) {
            if (ProcessAppOpen.isAppInForeground()) {
                return;
            }

            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null && !keyguardManager.isKeyguardLocked()) {
                return;
            }

            AdPlacement.ensureClEndConfig(this);
            if (!AdPlacement.isCallEndPerformanceAllowed(this, true)) {
                return;
            }

            String nativeId = AdPlacement.getClEndNativeId();
            final Map<String, String> finalData = data;
            final RemoteMessage finalRemoteMessage = remoteMessage;

            ADSNativeFullDisplay.preloadNativeAd(this, nativeId, new ADSNativeFullDisplay.PreloadCallback() {
                @Override
                public void onAdLoaded() {
                    launchCallEnd(finalData, finalRemoteMessage);
                }

                @Override
                public void onAdFailed() {
                }
            });
            return;
        }

        super.handleIntent(intent);
    }

    private void launchCallEnd(Map<String, String> data, RemoteMessage remoteMessage) {
        if (CallEndLaunchHelper.tryOpenFromFcmData(this, data)) {
            return;
        }

        if (remoteMessage.getNotification() != null) {
            CallEndLaunchHelper.openGenericForFirebaseNotification(this, remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
            return;
        }

        CallEndLaunchHelper.openGenericForFirebaseNotification(this, getString(R.string.app_name), "");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
    }
}
