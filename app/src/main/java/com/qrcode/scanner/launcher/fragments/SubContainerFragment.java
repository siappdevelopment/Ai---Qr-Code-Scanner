package com.qrcode.scanner.launcher.fragments;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.adapters.LauncherAppsAdapter;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.common.WeatherForecastGenerator;
import com.qrcode.scanner.launcher.helpers.LauncherAppsHelper;
import com.qrcode.scanner.launcher.helpers.LauncherAppsIconCache;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;
import com.qrcode.scanner.launcher.models.WeatherHourlyModel;
import com.qrcode.scanner.launcher.models.WeatherModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubContainerFragment extends Fragment {
    private AppCompatTextView tvGreeting, tvWeatherTemp, tvWeatherCondition, tvWeatherLocation;
    private CardView cvWeather;
    private AppCompatImageView ivWeatherIcon;
    private LinearLayout llWeatherHourly, llRecommendedApps;
    private RecyclerView rvRecommendedApps;

    private RelativeLayout rlNativeAdView;
    private ShimmerFrameLayout slNativeShimmer;
    private FrameLayout flNativeAd;

    private static final String LAUNCHER_NATIVE_AD_TYPE = "large";
    private static NativeAd cachedLauncherNativeAd;
    private static boolean nativeAdLoadInProgress;

    private int nativeAdRequestToken;

    private static final int MAX_RECOMMENDED_APPS = 4;
    private static final int RECOMMENDED_APPS_COLUMNS = 4;
    private static final String[] RECOMMENDED_APP_PACKAGES = {
            "com.whatsapp",
            "com.instagram.android",
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "com.google.android.youtube",
            "com.android.settings"
    };

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private boolean loadingRecommendedApps;
    private boolean loadingWeather;
    private boolean weatherLoadPosted;
    private LauncherAppsAdapter recommendedAppsAdapter;
    private ArrayList<LauncherAppsModel> recommendedAppsItems;
    private LauncherSettingsHelper.ChangeListener launcherSettingsChangeListener;

    private boolean isFragmentReady() {
        return isAdded() && getActivity() != null;
    }

    private boolean hasContext() {
        return isAdded() && getContext() != null;
    }

    @Nullable
    private LauncherAppsModel createAppModel(@NonNull PackageManager packageManager, @NonNull String packageName) {
        try {
            if (packageManager.getLaunchIntentForPackage(packageName) == null) {
                return null;
            }

            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            String appName = packageManager.getApplicationLabel(applicationInfo).toString();
            Drawable appIcon = packageManager.getApplicationIcon(applicationInfo);
            long installTime = 0L;
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                installTime = packageInfo.firstInstallTime;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            return new LauncherAppsModel(appName, packageName, appIcon, installTime);
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findIDs(view);
        hideNativeAdContainer();
        updateGreetingAndDate();
        showWeatherPlaceholder();
        setupRecyclerView();
        scheduleWeatherLoad();
        loadRecommendedAppsIfNeeded();
        launcherSettingsChangeListener = this::handleLauncherSettingsChanged;
        LauncherSettingsHelper.registerChangeListener(launcherSettingsChangeListener);
    }

    private void handleLauncherSettingsChanged(int changeMask) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            if ((changeMask & LauncherSettingsHelper.CHANGE_DISPLAY) != 0 && recommendedAppsAdapter != null) {
                recommendedAppsAdapter.applyDisplaySettings();
            }
            if ((changeMask & LauncherSettingsHelper.CHANGE_SERIALIZE) != 0) {
                applyRecommendedAppsSort();
            }
        });
    }

    private void applyRecommendedAppsSort() {
        if (recommendedAppsItems == null || recommendedAppsItems.isEmpty() || recommendedAppsAdapter == null || getContext() == null) {
            return;
        }
        ArrayList<LauncherAppsModel> previousOrder = new ArrayList<>(recommendedAppsItems);
        LauncherAppsHelper.sortApps(getContext(), recommendedAppsItems);
        recommendedAppsAdapter.notifyOrderChanged(previousOrder);
    }

    private void findIDs(@NonNull View view) {
        tvGreeting = view.findViewById(R.id.tvGreeting);
        View weatherRoot = view.findViewById(R.id.weatherCard);
        if (weatherRoot instanceof CardView) {
            cvWeather = (CardView) weatherRoot;
        }
        ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon);
        tvWeatherTemp = view.findViewById(R.id.tvWeatherTemp);
        tvWeatherCondition = view.findViewById(R.id.tvWeatherCondition);
        tvWeatherLocation = view.findViewById(R.id.tvWeatherLocation);
        llWeatherHourly = view.findViewById(R.id.llWeatherHourly);
        llRecommendedApps = view.findViewById(R.id.llRecommendedApps);
        rvRecommendedApps = view.findViewById(R.id.rvRecommendedApps);

        rlNativeAdView = view.findViewById(R.id.rlNativeAdView);
        slNativeShimmer = view.findViewById(R.id.slNativeShimmer);
        flNativeAd = view.findViewById(R.id.flNativeAd);
    }

    private void showAd() {
        if (rlNativeAdView == null || slNativeShimmer == null || flNativeAd == null || !isFragmentReady()) {
            return;
        }

        if (!isNativeAdEligible()) {
            hideNativeAdContainer();
            return;
        }

        if (hasDisplayedNativeContent()) {
            showNativeAdContentState();
            return;
        }

        if (nativeAdLoadInProgress) {
            showNativeLoadingState();
            return;
        }

        beginNativeAdLoad();
    }

    private boolean isNativeAdEligible() {
        if (!AdPlacement.getLauncherAppNativeAdShow()) {
            return false;
        }
        if (!hasContext() || !AdPlacement.canShowLauncherAppNativeAd(requireContext())) {
            return false;
        }
        return AdPlacement.isNetworkAvailable(requireActivity()) || AdPlacement.shouldUseQuizPriority();
    }

    private boolean shouldStartNativeAdLoad() {
        return isNativeAdEligible() && !nativeAdLoadInProgress;
    }

    private boolean hasDisplayedNativeContent() {
        return flNativeAd != null && flNativeAd.getChildCount() > 0;
    }

    private boolean isActiveNativeAdRequest(int requestToken) {
        return requestToken == nativeAdRequestToken;
    }

    /** Shimmer only — native slot stays GONE until content is ready. */
    private void showNativeLoadingState() {
        if (rlNativeAdView == null || slNativeShimmer == null || flNativeAd == null) {
            return;
        }
        rlNativeAdView.setVisibility(VISIBLE);
        flNativeAd.setVisibility(GONE);
        slNativeShimmer.setVisibility(VISIBLE);
        slNativeShimmer.startShimmer();
    }

    private void showNativeAdContentState() {
        if (rlNativeAdView == null || slNativeShimmer == null || flNativeAd == null) {
            return;
        }
        if (!hasDisplayedNativeContent()) {
            hideNativeAdContainer();
            return;
        }
        slNativeShimmer.stopShimmer();
        slNativeShimmer.setVisibility(GONE);
        rlNativeAdView.setVisibility(VISIBLE);
        flNativeAd.setVisibility(VISIBLE);
    }

    private void hideNativeAdContainer() {
        if (slNativeShimmer != null) {
            slNativeShimmer.stopShimmer();
            slNativeShimmer.setVisibility(GONE);
        }
        if (flNativeAd != null) {
            flNativeAd.setVisibility(GONE);
            flNativeAd.removeAllViews();
        }
        if (rlNativeAdView != null) {
            rlNativeAdView.setVisibility(GONE);
        }
    }

    private void clearNativeContainerViews() {
        if (flNativeAd != null) {
            flNativeAd.removeAllViews();
        }
    }

    private void beginNativeAdLoad() {
        if (!shouldStartNativeAdLoad()) {
            return;
        }
        final int requestToken = ++nativeAdRequestToken;
        destroyCachedNativeAd();
        clearNativeContainerViews();
        showNativeLoadingState();
        nativeAdLoadInProgress = true;

        AdPlacement.loadNativeAd(requireActivity(), AdPlacement.getLauncherAppNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, LAUNCHER_NATIVE_AD_TYPE, nativeAd -> {
            nativeAdLoadInProgress = false;
            if (!isActiveNativeAdRequest(requestToken) || !isFragmentReady()) {
                if (nativeAd != null) {
                    nativeAd.destroy();
                }
                return;
            }
            if (nativeAd != null) {
                if (cachedLauncherNativeAd != null && cachedLauncherNativeAd != nativeAd) {
                    cachedLauncherNativeAd.destroy();
                }
                cachedLauncherNativeAd = nativeAd;
            }
            if (hasDisplayedNativeContent()) {
                if (hasContext()) {
                    AdPlacement.setLauncherAppNativeLastShowTime(getContext(), System.currentTimeMillis());
                }
                showNativeAdContentState();
            } else {
                destroyCachedNativeAd();
                hideNativeAdContainer();
            }
        }, () -> {
            nativeAdLoadInProgress = false;
            if (!isActiveNativeAdRequest(requestToken) || !isFragmentReady()) {
                return;
            }
            destroyCachedNativeAd();
            clearNativeContainerViews();
            hideNativeAdContainer();
        });
    }

    private static void destroyCachedNativeAd() {
        if (cachedLauncherNativeAd != null) {
            cachedLauncherNativeAd.destroy();
            cachedLauncherNativeAd = null;
        }
    }

    public static void clearCachedNativeAd() {
        destroyCachedNativeAd();
        nativeAdLoadInProgress = false;
    }

    private void updateGreetingAndDate() {
        if (tvGreeting == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        tvGreeting.setText(greeting);
    }

    private void showWeatherPlaceholder() {
        if (cvWeather == null) {
            return;
        }
        cvWeather.setVisibility(VISIBLE);
        if (tvWeatherTemp != null) {
            tvWeatherTemp.setText("--°");
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText("Loading weather…");
        }
        if (tvWeatherLocation != null) {
            String location = Locale.getDefault().getDisplayCountry();
            tvWeatherLocation.setText(location == null || location.trim().isEmpty() ? "—" : location);
        }
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(R.drawable.ic_weather_partly_cloudy);
        }
        if (llWeatherHourly != null) {
            llWeatherHourly.setVisibility(GONE);
        }
    }

    private void scheduleWeatherLoad() {
        if (weatherLoadPosted || loadingWeather) {
            return;
        }
        WeatherModel cached = WeatherForecastGenerator.getCachedForToday();
        if (cached != null) {
            bindWeatherCard(cached);
            return;
        }
        weatherLoadPosted = true;
        loadingWeather = true;
        bgExecutor.execute(() -> {
            WeatherModel weather = WeatherForecastGenerator.generateAndCache();
            mainHandler.post(() -> {
                loadingWeather = false;
                weatherLoadPosted = false;
                if (!isAdded()) {
                    return;
                }
                bindWeatherCard(weather);
            });
        });
    }

    private void bindWeatherCard(@Nullable WeatherModel weather) {
        if (cvWeather == null) {
            return;
        }
        if (weather == null) {
            if (tvWeatherTemp != null) {
                tvWeatherTemp.setText("--°");
            }
            if (tvWeatherCondition != null) {
                tvWeatherCondition.setText("Weather unavailable");
            }
            if (llWeatherHourly != null) {
                llWeatherHourly.setVisibility(GONE);
            }
            return;
        }

        cvWeather.setVisibility(VISIBLE);
        if (tvWeatherTemp != null) {
            tvWeatherTemp.setText(weather.temperature);
        }
        if (tvWeatherCondition != null) {
            tvWeatherCondition.setText(weather.condition);
        }
        if (tvWeatherLocation != null) {
            tvWeatherLocation.setText(weather.location);
        }
        if (ivWeatherIcon != null) {
            ivWeatherIcon.setImageResource(weather.iconRes);
        }
        bindHourlyForecast(weather.hourlyForecast);
    }

    private void bindHourlyForecast(@NonNull List<WeatherHourlyModel> hourlyForecast) {
        if (llWeatherHourly == null || getContext() == null) {
            return;
        }
        llWeatherHourly.setVisibility(VISIBLE);
        llWeatherHourly.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (WeatherHourlyModel hour : hourlyForecast) {
            View itemView = inflater.inflate(R.layout.item_weather_hourly, llWeatherHourly, false);
            AppCompatTextView tvHourlyTime = itemView.findViewById(R.id.tvHourlyTime);
            AppCompatImageView ivHourlyIcon = itemView.findViewById(R.id.ivHourlyIcon);
            AppCompatTextView tvHourlyTemp = itemView.findViewById(R.id.tvHourlyTemp);
            if (tvHourlyTime != null) {
                tvHourlyTime.setText(hour.timeLabel);
            }
            if (ivHourlyIcon != null) {
                ivHourlyIcon.setImageResource(hour.iconRes);
            }
            if (tvHourlyTemp != null) {
                tvHourlyTemp.setText(hour.temperature);
            }
            llWeatherHourly.addView(itemView);
        }
    }

    private void setupRecyclerView() {
        if (rvRecommendedApps == null || getContext() == null) {
            return;
        }
        rvRecommendedApps.setItemAnimator(null);
        rvRecommendedApps.setNestedScrollingEnabled(false);
        rvRecommendedApps.setLayoutManager(new GridLayoutManager(requireContext(), RECOMMENDED_APPS_COLUMNS));
        if (recommendedAppsAdapter != null) {
            rvRecommendedApps.setAdapter(recommendedAppsAdapter);
        }
    }

    private void loadRecommendedAppsIfNeeded() {
        if (!isAdded() || loadingRecommendedApps) {
            return;
        }
        if (recommendedAppsAdapter != null && rvRecommendedApps != null && rvRecommendedApps.getAdapter() != null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            hideRecommendedAppsSection();
            return;
        }

        loadingRecommendedApps = true;
        Context appContext = context.getApplicationContext();
        bgExecutor.execute(() -> {
            ArrayList<LauncherAppsModel> recommendedApps = fetchRecommendedApps(appContext);
            int iconSizePx = AppUtils.dpToPx(appContext, LauncherSettingsHelper.getAppIconSize(appContext));
            int cornerRadiusPx = AppUtils.dpToPx(appContext, 12);
            LauncherAppsIconCache.warm(appContext, recommendedApps, iconSizePx, cornerRadiusPx);
            mainHandler.post(() -> applyRecommendedApps(recommendedApps));
        });
    }

    private ArrayList<LauncherAppsModel> fetchRecommendedApps(@NonNull Context context) {
        ArrayList<LauncherAppsModel> recommendedApps = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();

        for (String packageName : RECOMMENDED_APP_PACKAGES) {
            if (recommendedApps.size() >= MAX_RECOMMENDED_APPS) {
                break;
            }

            LauncherAppsModel appModel = createAppModel(packageManager, packageName);
            if (appModel != null) {
                recommendedApps.add(appModel);
            }
        }

        return recommendedApps;
    }

    private void applyRecommendedApps(@NonNull ArrayList<LauncherAppsModel> recommendedApps) {
        loadingRecommendedApps = false;
        if (!isAdded()) {
            return;
        }

        if (recommendedApps.isEmpty() || rvRecommendedApps == null || getContext() == null) {
            hideRecommendedAppsSection();
            return;
        }

        if (llRecommendedApps != null) {
            llRecommendedApps.setVisibility(VISIBLE);
        }
        recommendedAppsItems = recommendedApps;
        LauncherAppsHelper.sortApps(requireContext(), recommendedAppsItems);
        recommendedAppsAdapter = new LauncherAppsAdapter(requireContext(), recommendedAppsItems, null);
        rvRecommendedApps.setAdapter(recommendedAppsAdapter);
    }

    private void hideRecommendedAppsSection() {
        if (llRecommendedApps != null) {
            llRecommendedApps.setVisibility(GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGreetingAndDate();
        if (WeatherForecastGenerator.getCachedForToday() != null && tvWeatherTemp != null && "--°".equals(tvWeatherTemp.getText().toString())) {
            bindWeatherCard(WeatherForecastGenerator.getCachedForToday());
        }
        loadRecommendedAppsIfNeeded();
        showAd();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        nativeAdRequestToken++;
        nativeAdLoadInProgress = false;
        if (launcherSettingsChangeListener != null) {
            LauncherSettingsHelper.unregisterChangeListener(launcherSettingsChangeListener);
            launcherSettingsChangeListener = null;
        }
        recommendedAppsItems = null;
        clearNativeContainerViews();
        destroyCachedNativeAd();
        if (rvRecommendedApps != null) {
            rvRecommendedApps.setAdapter(null);
        }
        recommendedAppsAdapter = null;
        clearViewReferences();
        super.onDestroyView();
    }

    private void clearViewReferences() {
        tvGreeting = null;
        tvWeatherTemp = null;
        tvWeatherCondition = null;
        tvWeatherLocation = null;
        cvWeather = null;
        ivWeatherIcon = null;
        llWeatherHourly = null;
        llRecommendedApps = null;
        rvRecommendedApps = null;
        rlNativeAdView = null;
        slNativeShimmer = null;
        flNativeAd = null;
    }

    @Override
    public void onDestroy() {
        clearCachedNativeAd();
        bgExecutor.shutdownNow();
        super.onDestroy();
    }
}
