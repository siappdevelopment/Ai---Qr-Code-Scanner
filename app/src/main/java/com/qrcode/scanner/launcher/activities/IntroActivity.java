package com.qrcode.scanner.launcher.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AdTheme;
import com.qrcode.scanner.launcher.common.AnalyticsEvents;
import com.qrcode.scanner.launcher.common.IntroConfig;
import com.qrcode.scanner.launcher.common.IntroFullNativeAds;
import com.qrcode.scanner.launcher.common.IntroNavigation;
import com.qrcode.scanner.spapp.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Intro: ONE screen with a ViewPager2.
 * <pre>Intro1 -&gt; [full-screen native 1] -&gt; Intro2 -&gt; [full-screen native 2] -&gt; Intro3</pre>
 * <ul>
 *   <li>An ad page is inserted only while its native ad is LOADED (never a blank or loading page), and never in front
 *       of or under the page the user is already on. It is retried when the pager settles.</li>
 *   <li>The X of an ad page goes on. Auto swipe (Intro_NativeN_Auto_Sec) runs only while the user stays on that ad
 *       page.</li>
 *   <li>The bottom ad (Intro_Ad_*) belongs to the LAST intro page and starts loading only when the user reaches it:
 *       banner / mrec = full-width inline adaptive banner without side margins, native = medium card with margins.</li>
 *   <li>Back on page 0 finishes the intro, on later pages it goes one page back.</li>
 * </ul>
 */
public class IntroActivity extends AppCompatActivity {
    private static final int TYPE_INTRO = 0;
    private static final int TYPE_AD = 1;

    private static final int[] IMAGES = {R.drawable.intro_image1, R.drawable.intro_image2, R.drawable.intro_image3};
    private static final int[] TITLES = {R.string.intro_title_1, R.string.intro_title_2, R.string.intro_title_3};
    private static final int[] DESCS = {R.string.intro_desc_1, R.string.intro_desc_2, R.string.intro_desc_3};

    /** One pager page: an intro page (number 1..3) or the full-screen native ad of a slot (1..2). */
    private static final class Page {
        final int type;
        final int number;

        Page(int type, int number) {
            this.type = type;
            this.number = number;
        }
    }

    private final List<Page> pages = new ArrayList<>();
    private final Set<Integer> viewLogged = new HashSet<>();
    private final NativeAd[] adPageAds = new NativeAd[3];
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoNext = this::onAutoNext;

    private ViewPager2 vpIntro;
    private PageAdapter adapter;
    private AppCompatTextView btnNext;
    private AppCompatTextView btnNextTop;
    private RelativeLayout rlAdView;
    private RelativeLayout rlBannerAdView;
    private RelativeLayout rlNativeAdView;
    private ShimmerFrameLayout slBannerShimmer;
    private ShimmerFrameLayout slNativeShimmer;
    private LinearLayout llBannerAd;
    private FrameLayout flNativeAd;

    private int introCount;
    private boolean bottomAdRequested;
    private boolean bottomAdAllowed;
    private boolean syncPending;
    private boolean finished;
    private int autoScheduledFor = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        introCount = IntroNavigation.getEffectiveIntroButtonScreenCount();
        if (introCount <= 0) {
            finishIntro();
            return;
        }
        setContentView(R.layout.activity_intro);
        hideNavigationBar();
        findViews();

        for (int number = 1; number <= introCount; number++) {
            pages.add(new Page(TYPE_INTRO, number));
        }
        adapter = new PageAdapter();
        vpIntro.setAdapter(adapter);
        vpIntro.setOffscreenPageLimit(1);
        vpIntro.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                onPageShown(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE && syncPending) {
                    syncAdPages();
                }
            }
        });

        btnNext.setOnClickListener(view -> goNext());
        btnNextTop.setOnClickListener(view -> goNext());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int position = vpIntro.getCurrentItem();
                if (position <= 0) {
                    finishIntro();
                } else {
                    vpIntro.setCurrentItem(position - 1, true);
                }
            }
        });

        AdPlacement.preloadOnboardingInterstitialAd(this, true);
        // Both full-screen natives load now; an ad page appears when its ad is ready.
        IntroFullNativeAds.setOnLoadedListener(this::syncAdPages);
        IntroFullNativeAds.preloadAll(this);
        onPageShown(0);
    }

    private void findViews() {
        vpIntro = findViewById(R.id.vpIntro);
        btnNext = findViewById(R.id.btnNext);
        btnNextTop = findViewById(R.id.btnNextTop);
        rlAdView = findViewById(R.id.rlAdView);
        rlBannerAdView = findViewById(R.id.rlBannerAdView);
        slBannerShimmer = findViewById(R.id.slBannerShimmer);
        llBannerAd = findViewById(R.id.llBannerAd);
        rlNativeAdView = findViewById(R.id.rlNativeAdView);
        slNativeShimmer = findViewById(R.id.slNativeShimmer);
        flNativeAd = findViewById(R.id.flNativeAd);
    }

    private void hideNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    // ================================================================= pages

    private void onPageShown(int position) {
        if (position < 0 || position >= pages.size()) {
            return;
        }
        Page page = pages.get(position);
        handler.removeCallbacks(autoNext);
        autoScheduledFor = -1;
        if (page.type == TYPE_INTRO) {
            logIntroView(page.number);
        } else {
            scheduleAutoNext(position, page.number);
        }
        updateChrome(position);
    }

    private void logIntroView(int number) {
        if (!viewLogged.add(number)) {
            return;
        }
        String event = number == 1 ? AnalyticsEvents.INTRO_1_VIEW : number == 2 ? AnalyticsEvents.INTRO_2_VIEW : AnalyticsEvents.INTRO_3_VIEW;
        AnalyticsEvents.log(this, event);
    }

    /** Next button, indicator and bottom ad follow the page: nothing but the ad itself on an ad page. */
    private void updateChrome(int position) {
        Page page = pages.get(position);
        if (page.type == TYPE_AD) {
            btnNext.setVisibility(View.GONE);
            btnNextTop.setVisibility(View.GONE);
            rlAdView.setVisibility(View.GONE);
            return;
        }
        boolean last = page.number == introCount;
        if (last) {
            loadBottomAdOnce();
            rlAdView.setVisibility(bottomAdAllowed ? View.VISIBLE : View.GONE);
        } else {
            rlAdView.setVisibility(View.GONE);
        }
        // With 2 or 3 pages the LAST page shows its Next button at the top (room for the bottom ad).
        boolean nextOnTop = last && introCount >= 2;
        btnNextTop.setVisibility(nextOnTop ? View.VISIBLE : View.GONE);
        btnNext.setVisibility(nextOnTop ? View.GONE : View.VISIBLE);
    }

    private int indexOfIntroPage(int number) {
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            if (page.type == TYPE_INTRO && page.number == number) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasAdPage(int slot) {
        for (Page page : pages) {
            if (page.type == TYPE_AD && page.number == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the ad page of every slot whose native ad is loaded. An ad page goes right after its intro page and only
     * AFTER the page the user is on (never in front of or under it); while the pager scrolls it waits for the idle state.
     */
    private void syncAdPages() {
        if (isFinishing() || isDestroyed() || vpIntro == null) {
            return;
        }
        if (vpIntro.getScrollState() != ViewPager2.SCROLL_STATE_IDLE) {
            syncPending = true;
            return;
        }
        syncPending = false;
        int current = vpIntro.getCurrentItem();
        for (int slot = 1; slot <= 2; slot++) {
            if (hasAdPage(slot) || !IntroConfig.isNativeSlotEnabled(this, slot)) {
                continue;
            }
            int introIndex = indexOfIntroPage(slot);
            NativeAd ad = IntroFullNativeAds.peek(slot);
            if (introIndex < 0 || ad == null) {
                continue;
            }
            int insertAt = introIndex + 1;
            if (insertAt <= current) {
                continue;
            }
            ad = IntroFullNativeAds.take(slot);
            if (ad == null) {
                continue;
            }
            ad.setOnPaidEventListener(adValue -> AdPlacement.logAdRevenue(this, adValue));
            adPageAds[slot] = ad;
            pages.add(insertAt, new Page(TYPE_AD, slot));
            adapter.notifyItemInserted(insertAt);
        }
    }

    // ================================================================= navigation

    private void goNext() {
        if (finished) {
            return;
        }
        int position = vpIntro.getCurrentItem();
        Page page = pages.get(position);
        boolean lastIntro = page.type == TYPE_INTRO && page.number == introCount;
        if (lastIntro || position + 1 >= pages.size()) {
            finishIntro();
        } else {
            vpIntro.setCurrentItem(position + 1, true);
        }
    }

    private void finishIntro() {
        if (finished) {
            return;
        }
        finished = true;
        IntroNavigation.finishIntro(this);
    }

    /** Auto swipe of an ad page: only while the user is still on it. */
    private void scheduleAutoNext(int position, int slot) {
        int seconds = IntroConfig.getNativeAutoSec(this, slot);
        if (seconds <= 0) {
            return;
        }
        autoScheduledFor = position;
        handler.postDelayed(autoNext, seconds * 1000L);
    }

    private void onAutoNext() {
        int position = vpIntro == null ? -1 : vpIntro.getCurrentItem();
        if (isFinishing() || isDestroyed() || position != autoScheduledFor || position < 0 || position >= pages.size()
                || pages.get(position).type != TYPE_AD) {
            return;
        }
        goNext();
    }

    // ================================================================= bottom ad (last intro page)

    /** Starts the bottom ad the first time the last intro page is reached. */
    private void loadBottomAdOnce() {
        if (bottomAdRequested) {
            return;
        }
        bottomAdRequested = true;
        boolean quiz = AdPlacement.shouldUseQuizPriority();
        if (!AdPlacement.getIntroAdShow() || (!quiz && !AdPlacement.isNetworkAvailable(this))) {
            bottomAdAllowed = false;
            return;
        }
        bottomAdAllowed = true;
        rlAdView.setVisibility(View.VISIBLE);
        String type = AdPlacement.getIntroAdType() == null ? "" : AdPlacement.getIntroAdType().trim().toLowerCase(Locale.ROOT);
        if ("banner".equals(type) || "mrec".equals(type)) {
            // Full-width inline adaptive banner, no side margins; the loaders hide the area when it fails.
            rlBannerAdView.setVisibility(View.VISIBLE);
            rlNativeAdView.setVisibility(View.GONE);
            AdPlacement.loadAdaptiveBannerAd(this, AdPlacement.getIntroBannerId(), rlBannerAdView, slBannerShimmer, llBannerAd, false, true, null, 160);
        } else {
            // Medium native card with side margins.
            rlBannerAdView.setVisibility(View.GONE);
            rlNativeAdView.setVisibility(View.VISIBLE);
            AdPlacement.loadNativeAd(this, AdPlacement.getIntroNativeId(), rlNativeAdView, slNativeShimmer, flNativeAd, "medium",
                    null, null, false, AdPlacement.onboardingNativeColor(this));
        }
    }

    // ================================================================= lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        hideNavigationBar();
        // Coming back to an ad page restarts its auto swipe.
        if (vpIntro != null && !pages.isEmpty()) {
            int position = vpIntro.getCurrentItem();
            if (position < pages.size() && pages.get(position).type == TYPE_AD && autoScheduledFor != position) {
                scheduleAutoNext(position, pages.get(position).number);
            }
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(autoNext);
        autoScheduledFor = -1;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(autoNext);
        IntroFullNativeAds.setOnLoadedListener(null);
        for (int slot = 1; slot < adPageAds.length; slot++) {
            if (adPageAds[slot] != null) {
                adPageAds[slot].destroy();
                adPageAds[slot] = null;
            }
        }
        super.onDestroy();
    }

    // ================================================================= adapter

    private final class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemViewType(int position) {
            return pages.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == TYPE_AD ? R.layout.item_intro_native_ad : R.layout.item_intro_page;
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Page page = pages.get(position);
            if (page.type == TYPE_AD) {
                bindAdPage(holder.itemView, page.number);
            } else {
                bindIntroPage(holder.itemView, page.number);
            }
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }
    }

    private void bindIntroPage(@NonNull View view, int number) {
        int index = Math.max(0, Math.min(number - 1, IMAGES.length - 1));
        ((AppCompatImageView) view.findViewById(R.id.ivIntro)).setImageResource(IMAGES[index]);
        ((AppCompatTextView) view.findViewById(R.id.tvTitle)).setText(TITLES[index]);
        ((AppCompatTextView) view.findViewById(R.id.tvDesc)).setText(DESCS[index]);
        IntroNavigation.setupIntroButtonIndicators(this, view.findViewById(R.id.llIndicator), number);
    }

    private void bindAdPage(@NonNull View view, int slot) {
        FrameLayout container = view.findViewById(R.id.flFullAd);
        container.removeAllViews();
        NativeAd ad = adPageAds[slot];
        if (ad == null) {
            return;
        }
        NativeAdView adView = (NativeAdView) LayoutInflater.from(AdTheme.forLauncher(this)).inflate(R.layout.native_full_ad_layout, container, false);
        AdPlacement.populateNativeAdView(ad, adView, "full");
        View close = adView.findViewById(R.id.ivClose);
        if (close != null) {
            close.setVisibility(View.VISIBLE);
            close.bringToFront();
            close.setClickable(true);
            close.setOnClickListener(v -> goNext());
        }
        container.addView(adView);
    }
}
