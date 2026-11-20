package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.net.Uri;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Source splash referrer read. It does not block Remote Config or navigation.
 */
public final class InstallReferrerStartup {
    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();

    private InstallReferrerStartup() {
    }

    public static void start(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            Uri referrer = activity.getReferrer();
            if (referrer != null && activity.getPreferences(Activity.MODE_PRIVATE).getBoolean(referrer.toString(), false)) {
                return;
            }
            InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(activity.getApplicationContext()).build();
            BACKGROUND.execute(() -> connect(activity.getApplicationContext(), referrerClient));
        } catch (Exception ignored) {
        }
    }

    private static void connect(android.content.Context context, InstallReferrerClient referrerClient) {
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        ReferrerDetails response = referrerClient.getInstallReferrer();
                        AdPlacement.setReferrerUrl(context, response.getInstallReferrer());
                    } catch (RemoteException ignored) {
                    }
                    referrerClient.endConnection();
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
            }
        });
    }
}
