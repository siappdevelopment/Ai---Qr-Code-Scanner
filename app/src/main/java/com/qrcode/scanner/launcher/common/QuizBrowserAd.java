package com.qrcode.scanner.launcher.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.qrcode.scanner.spapp.R;

/**
 * The {@code Quiz_Browser} ad: the Quiz link opens inside the app as a whole-screen web page, with nothing else on the
 * screen (no title, no close button, only a thin progress bar). Back closes it. A store link (market:, intent:) leaves for
 * the store, normal pages stay inside. The dialog and its WebView are released on dismiss.
 */
public final class QuizBrowserAd {
    private static final String TAG = "QuizBrowserAd";
    private static final int FULL_PROGRESS = 100;

    private QuizBrowserAd() {
    }

    /**
     * Shows {@code link} in the dialog; {@code onDone} runs exactly once, when the dialog is dismissed. Returns false (and
     * never calls {@code onDone}) when the activity is finishing, the link is not a web page, or anything fails.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static boolean show(@NonNull Activity activity, @Nullable String link, @NonNull Runnable onDone) {
        final String url = link == null ? "" : link.trim();
        if (activity.isFinishing() || activity.isDestroyed() || !(url.startsWith("https://") || url.startsWith("http://"))) {
            return false;
        }
        try {
            int theme = R.style.Theme_NativeFullAd;
            Context themed = new ContextThemeWrapper(activity, theme);
            final Dialog dialog = new Dialog(activity, theme);
            View root = LayoutInflater.from(themed).inflate(R.layout.dialog_quiz_browser, null, false);
            dialog.setContentView(root);
            dialog.setCancelable(true);

            final WebView web = root.findViewById(R.id.qzBrowserWeb);
            final ProgressBar progress = root.findViewById(R.id.qzBrowserProgress);
            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setSupportZoom(false);
            settings.setSupportMultipleWindows(false);
            // The page is third-party content: no local file / content access.
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            web.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    progress.setProgress(newProgress);
                    progress.setVisibility(newProgress >= FULL_PROGRESS ? View.GONE : View.VISIBLE);
                }
            });
            web.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri target = request == null ? null : request.getUrl();
                    if (target == null) {
                        return true;
                    }
                    String scheme = target.getScheme();
                    if ("http".equals(scheme) || "https".equals(scheme)) {
                        return false;
                    }
                    openOutside(activity, target);
                    return true;
                }

                @Override
                public void onPageStarted(WebView view, String pageUrl, Bitmap favicon) {
                    progress.setVisibility(View.VISIBLE);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    // A page that cannot load must not leave a blank ad: the main page failing closes the dialog.
                    if (request != null && request.isForMainFrame()) {
                        Log.w(TAG, "the page failed to load (" + (error == null ? "" : error.getDescription()) + "), the ad closes");
                        try {
                            dialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
            dialog.setOnDismissListener(d -> {
                if (web.getParent() instanceof ViewGroup) {
                    ((ViewGroup) web.getParent()).removeView(web);
                }
                web.stopLoading();
                web.loadUrl("about:blank");
                web.destroy();
                onDone.run();
            });
            dialog.show();
            styleWindow(dialog, root);
            web.loadUrl(url);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Quiz browser ad failed", e);
            return false;
        }
    }

    /** market:, intent:, tel: ... leave for the system; the page stays. An intent: link must not target a private component. */
    private static void openOutside(Activity activity, Uri uri) {
        try {
            Intent intent = "intent".equals(uri.getScheme())
                    ? Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                    : new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.setComponent(null);
            intent.setSelector(null);
            activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }

    /** Edge-to-edge dialog window in white; the content keeps clear of the system bars. */
    private static void styleWindow(Dialog dialog, View root) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        int color = Color.WHITE;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setBackgroundDrawable(new ColorDrawable(color));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(color);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
