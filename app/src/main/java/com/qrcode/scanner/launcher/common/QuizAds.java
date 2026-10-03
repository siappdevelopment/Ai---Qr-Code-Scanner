package com.qrcode.scanner.launcher.common;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.qrcode.scanner.app.R;
import com.qrcode.scanner.launcher.remote.RemoteConfigValues;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Source quiz ad layouts and click behavior. A missing list or failed image keeps the user moving.
 */
public final class QuizAds {
    private static final int IMAGE_FALLBACK = R.mipmap.ic_launcher;
    private static final String[] FALLBACK_RATES = {"4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9"};
    private static final String[] USERS = {"50K+ Users", "100K+ Users", "500K+ Users", "1M+ Users"};
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newCachedThreadPool();

    private QuizAds() {
    }

    public static boolean showBanner(Activity activity, RelativeLayout container, ShimmerFrameLayout shimmer, LinearLayout content) {
        try {
            int itemCount = RemoteConfigValues.getQuizSyncedItemCount();
            if (itemCount <= 0 || activity == null || container == null || content == null) {
                return false;
            }
            if (shimmer != null) {
                shimmer.setVisibility(View.GONE);
            }
            content.removeAllViews();
            View view = LayoutInflater.from(content.getContext()).inflate(R.layout.qz_banner_ad, content, false);
            int index = new Random().nextInt(itemCount);
            bindText(view, index, RemoteConfigValues.getQuizBannerTitleList(), RemoteConfigValues.getQuizBannerDescriptionList());
            loadImage(view, view.findViewById(R.id.ivQZAppIcon), RemoteConfigValues.getQuizAppIconList(), index, R.id.qzShimmerIcon);
            wireLink(activity, view, view.findViewById(R.id.btnQZClick));
            content.addView(view);
            content.setVisibility(View.VISIBLE);
            container.setVisibility(View.VISIBLE);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean showNative(Activity activity, RelativeLayout container, ShimmerFrameLayout shimmer, FrameLayout content, @Nullable String type) {
        try {
            int itemCount = RemoteConfigValues.getQuizSyncedItemCount();
            if (itemCount <= 0 || activity == null || container == null || content == null) {
                return false;
            }
            if (shimmer != null) {
                shimmer.stopShimmer();
                shimmer.setVisibility(View.GONE);
            }
            content.removeAllViews();
            View view = LayoutInflater.from(content.getContext()).inflate(nativeLayout(type), content, false);
            int index = new Random().nextInt(itemCount);
            bindText(view, index, RemoteConfigValues.getQuizNativeTitleList(), RemoteConfigValues.getQuizNativeDescriptionList());
            loadImage(view, view.findViewById(R.id.ivQZAppIcon), RemoteConfigValues.getQuizAppIconList(), index, R.id.qzShimmerIcon);
            AppCompatImageView media = view.findViewById(R.id.ivQZAppMedia);
            if (media != null) {
                loadImage(view, media, RemoteConfigValues.getQuizNativeMediaList(), index, R.id.qzShimmer);
            }
            wireLink(activity, view, view.findViewById(R.id.btnQZClick));
            content.addView(view);
            content.setVisibility(View.VISIBLE);
            container.setVisibility(View.VISIBLE);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean showInterstitial(Activity activity, @Nullable Runnable onDismiss) {
        try {
            int itemCount = RemoteConfigValues.getQuizSyncedItemCount();
            if (itemCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }
            Dialog dialog = fullscreenDialog(activity, R.layout.qz_interstitial_ad, R.style.Theme_QuizInterstitial);
            View root = ((android.view.ViewGroup) dialog.getWindow().getDecorView()).getChildAt(0);
            int index = new Random().nextInt(itemCount);
            bindText(root, index, RemoteConfigValues.getQuizInterstitialTitleList(), RemoteConfigValues.getQuizInterstitialDescriptionList());
            loadImage(root, root.findViewById(R.id.ivQZAppIcon), RemoteConfigValues.getQuizAppIconList(), index, R.id.qzShimmerIcon);
            loadImage(root, root.findViewById(R.id.ivQZAppMedia), RemoteConfigValues.getQuizInterstitialMediaList(), index, R.id.qzShimmer);
            bindRating(root, index);
            AppCompatTextView users = root.findViewById(R.id.tvQZUser);
            if (users != null) {
                users.setText(USERS[index % USERS.length]);
            }
            AppCompatTextView timer = root.findViewById(R.id.tvQZTimer);
            AppCompatImageView closeIcon = root.findViewById(R.id.ivQZClose);
            AppCompatTextView closeButton = root.findViewById(R.id.btnQZClose);
            if (timer != null) {
                timer.setVisibility(View.GONE);
            }
            if (closeIcon != null) {
                closeIcon.setVisibility(View.VISIBLE);
            }
            if (closeButton != null) {
                closeButton.setEnabled(true);
                closeButton.setAlpha(1f);
            }
            AtomicBoolean finished = new AtomicBoolean(false);
            Runnable dismiss = () -> {
                if (!finished.compareAndSet(false, true)) {
                    return;
                }
                try {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception ignored) {
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
            };
            if (closeIcon != null) {
                closeIcon.setOnClickListener(v -> dismiss.run());
            }
            if (closeButton != null) {
                closeButton.setOnClickListener(v -> {
                    if (closeButton.isEnabled()) {
                        dismiss.run();
                    }
                });
            }
            wireLink(activity, root, root.findViewById(R.id.btnQZClick));
            final int previousStatusColor = activity.getWindow().getStatusBarColor();
            final int previousNavigationColor = activity.getWindow().getNavigationBarColor();
            dialog.setOnShowListener(shown -> matchQuizSystemBars(activity, dialog.getWindow()));
            dialog.setOnDismissListener(dismissed -> restoreHostSystemBars(activity, previousStatusColor, previousNavigationColor));
            dialog.show();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean showAppOpen(Activity activity, @Nullable Runnable onDismiss) {
        try {
            int itemCount = RemoteConfigValues.getQuizSyncedItemCount();
            if (itemCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }
            Dialog dialog = fullscreenDialog(activity, R.layout.qz_app_open_ad);
            View root = ((android.view.ViewGroup) dialog.getWindow().getDecorView()).getChildAt(0);
            int index = new Random().nextInt(itemCount);
            loadImage(root, root.findViewById(R.id.ivQZAppIcon), RemoteConfigValues.getQuizAppIconList(), index, R.id.qzShimmerIcon);
            loadImage(root, root.findViewById(R.id.ivQZAppMedia), RemoteConfigValues.getQuizAppOpenMediaList(), index, R.id.qzShimmer);
            applyButton(root.findViewById(R.id.btnQZClick));
            Runnable dismiss = () -> {
                try {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                } catch (Exception ignored) {
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
            };
            View close = root.findViewById(R.id.llQZClose);
            if (close != null) {
                close.setOnClickListener(v -> dismiss.run());
            }
            AppCompatImageView media = root.findViewById(R.id.ivQZAppMedia);
            if (media != null) {
                media.setOnClickListener(v -> openLink(activity, RemoteConfigValues.pickQuizLink()));
            }
            AppCompatTextView button = root.findViewById(R.id.btnQZClick);
            if (button != null) {
                button.setOnClickListener(v -> openLink(activity, RemoteConfigValues.pickQuizLink()));
            }
            dialog.show();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean showNativeFull(Activity activity, @Nullable Runnable onDismiss) {
        try {
            int itemCount = RemoteConfigValues.getQuizSyncedItemCount();
            if (itemCount <= 0 || activity == null || activity.isFinishing()) {
                return false;
            }
            Dialog dialog = fullscreenDialog(activity, R.layout.qz_native_full_ad, R.style.Theme_NativeFullAd);
            if (dialog.getWindow() == null) {
                return false;
            }
            View root = quizInterstitialRoot(dialog.getWindow());
            if (root == null) {
                return false;
            }
            int index = new Random().nextInt(itemCount);
            bindText(root, index, RemoteConfigValues.getQuizNativeTitleList(), RemoteConfigValues.getQuizNativeDescriptionList());
            loadImage(root, root.findViewById(R.id.ivQZAppIcon), RemoteConfigValues.getQuizAppIconList(), index, R.id.qzShimmerIcon);
            loadImage(root, root.findViewById(R.id.ivQZAppMedia), RemoteConfigValues.getQuizNativeMediaList(), index, R.id.qzShimmer);
            wireLink(activity, root, root.findViewById(R.id.btnQZClick));
            View close = root.findViewById(R.id.ivQZClose);
            if (close == null) {
                close = root.findViewById(R.id.ivClose);
            }
            if (close != null) {
                close.bringToFront();
                close.setClickable(true);
                close.setOnClickListener(v -> {
                    try {
                        dialog.dismiss();
                    } catch (Exception ignored) {
                    }
                });
            }
            Window host = activity.getWindow();
            final int savedStatusColor = host.getStatusBarColor();
            final int savedNavColor = host.getNavigationBarColor();
            WindowInsetsControllerCompat hostController = WindowCompat.getInsetsController(host, host.getDecorView());
            final boolean savedLightStatus = hostController.isAppearanceLightStatusBars();
            final boolean savedLightNav = hostController.isAppearanceLightNavigationBars();
            final boolean savedStatusContrast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && host.isStatusBarContrastEnforced();
            final boolean savedNavContrast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && host.isNavigationBarContrastEnforced();
            dialog.setOnDismissListener(d -> {
                host.setStatusBarColor(savedStatusColor);
                host.setNavigationBarColor(savedNavColor);
                hostController.setAppearanceLightStatusBars(savedLightStatus);
                hostController.setAppearanceLightNavigationBars(savedLightNav);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    host.setStatusBarContrastEnforced(savedStatusContrast);
                    host.setNavigationBarContrastEnforced(savedNavContrast);
                }
                if (onDismiss != null) {
                    onDismiss.run();
                }
            });
            matchQuizNativeFullBars(activity, dialog.getWindow(), root);
            dialog.show();
            matchQuizNativeFullBars(activity, dialog.getWindow(), root);
            if (close != null) {
                close.bringToFront();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void openBrowserThenContinue(Activity activity, @Nullable Runnable onReturn) {
        String link = RemoteConfigValues.pickQuizLink();
        if (activity == null || link.isEmpty()) {
            if (onReturn != null) {
                onReturn.run();
            }
            return;
        }
        AtomicBoolean completed = new AtomicBoolean(false);
        Application application = activity.getApplication();
        Application.ActivityLifecycleCallbacks callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity a, android.os.Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity a) {
            }

            @Override
            public void onActivityResumed(Activity a) {
                if (a == activity && completed.compareAndSet(false, true)) {
                    application.unregisterActivityLifecycleCallbacks(this);
                    if (onReturn != null) {
                        onReturn.run();
                    }
                }
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivityStopped(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
            }
        };
        application.registerActivityLifecycleCallbacks(callbacks);
        try {
            openLink(activity, link);
        } catch (Exception ignored) {
            application.unregisterActivityLifecycleCallbacks(callbacks);
            if (onReturn != null && completed.compareAndSet(false, true)) {
                onReturn.run();
            }
        }
    }

    public static void openLink(@Nullable Activity activity, @Nullable String link) {
        if (activity == null || link == null || link.trim().isEmpty()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private static int nativeLayout(@Nullable String type) {
        if ("large".equalsIgnoreCase(type)) {
            return R.layout.qz_native_large_ad;
        }
        if ("medium".equalsIgnoreCase(type)) {
            return R.layout.qz_native_medium_ad;
        }
        if ("intro".equalsIgnoreCase(type)) {
            return R.layout.qz_native_intro_ad;
        }
        if ("full".equalsIgnoreCase(type)) {
            return R.layout.qz_native_full_ad;
        }
        return R.layout.qz_native_small_ad;
    }

    /** Same status and navigation treatment as the Google Native full ad. */
    private static void matchQuizNativeFullBars(Activity activity, @Nullable Window window, @NonNull View content) {
        if (window == null) {
            return;
        }
        int color = ContextCompat.getColor(activity, R.color.surface_primary);
        boolean night = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setBackgroundDrawable(new ColorDrawable(color));
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(color);
        window.getDecorView().setPadding(0, 0, 0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(attrs);
            window.setNavigationBarDividerColor(Color.TRANSPARENT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        content.setBackgroundColor(color);
        if (!(content.getTag() instanceof int[])) {
            content.setTag(new int[]{content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), content.getPaddingBottom()});
        }
        final int[] basePadding = (int[]) content.getTag();
        View close = content.findViewById(R.id.ivClose);
        if (close != null && !(close.getTag() instanceof Integer) && close.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            close.setTag(((ViewGroup.MarginLayoutParams) close.getLayoutParams()).topMargin);
        }
        final int closeBaseTop = close != null && close.getTag() instanceof Integer ? (Integer) close.getTag() : 0;
        View statusScrim = content.findViewById(R.id.statusBarScrim);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(basePadding[0], basePadding[1], basePadding[2], basePadding[3] + bars.bottom);
            if (close != null && close.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) close.getLayoutParams();
                lp.topMargin = closeBaseTop + bars.top;
                close.setLayoutParams(lp);
            }
            if (statusScrim != null) {
                ViewGroup.LayoutParams scrimParams = statusScrim.getLayoutParams();
                scrimParams.height = night ? bars.top : 0;
                statusScrim.setLayoutParams(scrimParams);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(window.getDecorView());
        ViewCompat.requestApplyInsets(content);
        applyQuizNativeBarIcons(window, night, color);
        applyQuizNativeBarIcons(activity.getWindow(), night, color);
        window.getDecorView().post(() -> {
            applyQuizNativeBarIcons(window, night, color);
            applyQuizNativeBarIcons(activity.getWindow(), night, color);
        });
    }

    private static void applyQuizNativeBarIcons(@Nullable Window window, boolean night, int navigationColor) {
        if (window == null) {
            return;
        }
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(navigationColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        boolean lightIcons = !night;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(lightIcons);
        controller.setAppearanceLightNavigationBars(lightIcons);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                int mask = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                insetsController.setSystemBarsAppearance(lightIcons ? mask : 0, mask);
            }
        }
    }

    private static void matchQuizSystemBars(Activity activity, @Nullable Window window) {
        if (activity == null || window == null) {
            return;
        }
        boolean isNight = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int color = ContextCompat.getColor(activity, R.color.surface_primary);
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.setBackgroundDrawable(new ColorDrawable(color));
        window.setStatusBarColor(color);
        window.setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
        View layout = quizInterstitialRoot(window);
        if (layout != null) {
            layout.setBackgroundColor(color);
            if (!(layout.getTag() instanceof int[])) {
                layout.setTag(new int[]{layout.getPaddingLeft(), layout.getPaddingTop(), layout.getPaddingRight(), layout.getPaddingBottom()});
            }
            final int[] basePadding = (int[]) layout.getTag();
            ViewCompat.setOnApplyWindowInsetsListener(layout, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setBackgroundColor(color);
                view.setPadding(basePadding[0], basePadding[1] + bars.top, basePadding[2], basePadding[3] + bars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
            ViewCompat.requestApplyInsets(layout);
        }
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(!isNight);
        controller.setAppearanceLightNavigationBars(!isNight);
        Window host = activity.getWindow();
        host.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        host.setStatusBarColor(color);
        host.setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            host.setStatusBarContrastEnforced(false);
            host.setNavigationBarContrastEnforced(false);
        }
        WindowInsetsControllerCompat hostController = WindowCompat.getInsetsController(host, host.getDecorView());
        hostController.setAppearanceLightStatusBars(!isNight);
        hostController.setAppearanceLightNavigationBars(!isNight);
    }

    private static void restoreHostSystemBars(Activity activity, int statusColor, int navigationColor) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        Window host = activity.getWindow();
        host.setStatusBarColor(statusColor);
        host.setNavigationBarColor(navigationColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            host.setStatusBarContrastEnforced(true);
            host.setNavigationBarContrastEnforced(true);
        }
    }

    @Nullable
    private static View quizInterstitialRoot(@NonNull Window window) {
        View content = window.findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            return ((ViewGroup) content).getChildAt(0);
        }
        return content;
    }

    private static Dialog fullscreenDialog(Activity activity, int layout) {
        return fullscreenDialog(activity, layout, 0);
    }

    private static Dialog fullscreenDialog(Activity activity, int layout, int themeRes) {
        Dialog dialog = themeRes == 0 ? new Dialog(activity) : new Dialog(activity, themeRes);
        if (themeRes == 0) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        dialog.setContentView(layout);
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            if (themeRes == 0) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        }
        return dialog;
    }

    private static void bindText(View root, int index, List<String> titles, List<String> descriptions) {
        AppCompatTextView title = root.findViewById(R.id.tvQZAppTitle);
        AppCompatTextView description = root.findViewById(R.id.tvQZAppDescription);
        if (title != null) {
            title.setText(RemoteConfigValues.quizItem(titles, index));
        }
        if (description != null) {
            description.setText(RemoteConfigValues.quizItem(descriptions, index));
        }
        applyButton(root.findViewById(R.id.btnQZClick));
        applyColors(root);
    }

    private static void applyButton(@Nullable AppCompatTextView button) {
        if (button == null) {
            return;
        }
        String text = RemoteConfigValues.getQuizButtonText();
        if (!text.isEmpty()) {
            button.setText(text);
        }
    }

    private static void applyColors(View root) {
        View label = root.findViewById(R.id.tvQZLabel);
        if (label != null && label.getBackground() != null) {
            tint(label, AdPlacement.getNativeAdLabelColor());
        }
        View button = root.findViewById(R.id.btnQZClick);
        if (button != null && button.getBackground() != null) {
            tint(button, AdPlacement.getNativeAdButtonColor());
        }
    }

    private static void tint(View view, String colorHex) {
        if (colorHex == null || colorHex.trim().isEmpty()) {
            return;
        }
        try {
            String value = colorHex.trim();
            if (!value.startsWith("#")) {
                value = "#" + value;
            }
            DrawableCompat.setTint(DrawableCompat.wrap(view.getBackground().mutate()), Color.parseColor(value));
        } catch (Exception ignored) {
        }
    }

    private static void bindRating(View root, int index) {
        AppCompatTextView rate = root.findViewById(R.id.tvQZRate);
        AppCompatRatingBar bar = root.findViewById(R.id.rbQZRating);
        if (rate == null || bar == null) {
            return;
        }
        String raw = RemoteConfigValues.quizItem(RemoteConfigValues.getQuizInterstitialRateList(), index);
        if (raw.isEmpty() && index >= 0 && index < FALLBACK_RATES.length) {
            raw = FALLBACK_RATES[index];
        }
        float rating = 4.5f;
        try {
            if (!raw.isEmpty()) {
                rating = Float.parseFloat(raw);
            }
        } catch (Exception ignored) {
        }
        rating = Math.max(0f, Math.min(5f, rating));
        rate.setText(String.valueOf(rating));
        bar.setMax(5);
        bar.setNumStars(5);
        bar.setStepSize(0.1f);
        bar.setIsIndicator(true);
        bar.setRating(rating);
    }

    private static void wireLink(Activity activity, View root, @Nullable View button) {
        View.OnClickListener listener = v -> openLink(activity, RemoteConfigValues.pickQuizLink());
        root.setOnClickListener(listener);
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }

    private static void loadImage(@Nullable View root, @Nullable AppCompatImageView imageView, @Nullable List<String> urls, int index, int shimmerId) {
        if (imageView == null) {
            return;
        }
        ShimmerFrameLayout shimmer = root == null ? null : root.findViewById(shimmerId);
        String imageUrl = RemoteConfigValues.quizItem(urls, index).trim();
        if (imageUrl.isEmpty()) {
            stopShimmer(shimmer);
            imageView.setImageResource(IMAGE_FALLBACK);
            imageView.setVisibility(View.VISIBLE);
            return;
        }
        if (shimmer != null) {
            shimmer.setVisibility(View.VISIBLE);
            shimmer.startShimmer();
        }
        imageView.setTag(imageUrl);
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = download(imageUrl);
            new Handler(Looper.getMainLooper()).post(() -> {
                Object tag = imageView.getTag();
                if (!(tag instanceof String) || !imageUrl.equals(tag)) {
                    return;
                }
                stopShimmer(shimmer);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(IMAGE_FALLBACK);
                }
                imageView.setVisibility(View.VISIBLE);
            });
        });
    }

    private static void stopShimmer(@Nullable ShimmerFrameLayout shimmer) {
        if (shimmer == null) {
            return;
        }
        shimmer.stopShimmer();
        shimmer.setVisibility(View.GONE);
    }

    @Nullable
    private static Bitmap download(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setDoInput(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
