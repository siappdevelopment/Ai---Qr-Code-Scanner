package com.qrcode.scanner.launcher.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.R;
import com.qrcode.scanner.launcher.common.AdPlacement;
import com.qrcode.scanner.launcher.common.AppUtils;
import com.qrcode.scanner.launcher.dialogs.LauncherAppsBottomSheet;
import com.qrcode.scanner.launcher.helpers.LauncherAppsIconCache;
import com.qrcode.scanner.launcher.helpers.LauncherQuizGames;
import com.qrcode.scanner.launcher.helpers.LauncherSettingsHelper;
import com.qrcode.scanner.launcher.interfaces.OnLauncherAppLongClickListener;
import com.qrcode.scanner.launcher.models.LauncherAppsDisplayItem;
import com.qrcode.scanner.launcher.models.LauncherAppsModel;
import com.qrcode.scanner.launcher.models.QuizGameItem;

import java.util.ArrayList;
import java.util.List;

public class LauncherAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final Object PAYLOAD_DISPLAY = new Object();
    private static final int MAX_QUIZ_ITEMS = 40;
    private static final int LIST_NATIVE_AFTER_ITEMS = 8;

    private final Context context;
    private final List<LauncherAppsModel> items;
    private final OnLauncherAppLongClickListener longClickListener;
    private final boolean injectQuizIcons;
    private final List<LauncherAppsDisplayItem> displayItems = new ArrayList<>();
    private boolean quizIconsVisible = true;
    private boolean listNativePlaceholderEnabled;
    private boolean listNativeShimmerVisible;
    private int iconSizePx;
    private int labelSizeSp;
    private boolean labelVisible;

    public LauncherAppsAdapter(Context context, List<LauncherAppsModel> items, OnLauncherAppLongClickListener longClickListener) {
        this(context, items, longClickListener, false);
    }

    public LauncherAppsAdapter(Context context, List<LauncherAppsModel> items, OnLauncherAppLongClickListener longClickListener, boolean injectQuizIcons) {
        this.context = context;
        this.items = items;
        this.longClickListener = longClickListener;
        this.injectQuizIcons = injectQuizIcons;
        refreshSizeVisibility();
        rebuildDisplayItems();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == LauncherAppsDisplayItem.TYPE_NATIVE) {
            View spacer = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_launcher_native_spacer, parent, false);
            return new NativeSpacerViewHolder(spacer);
        }
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_launcher_apps, parent, false));
        holder.ivAppIcon.setScaleType(AppCompatImageView.ScaleType.CENTER_CROP);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (holder instanceof NativeSpacerViewHolder) {
            bindNativeSpacer((NativeSpacerViewHolder) holder);
            return;
        }
        if (!(holder instanceof ViewHolder)) {
            return;
        }
        ViewHolder appHolder = (ViewHolder) holder;
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        LauncherAppsDisplayItem item = displayItems.get(position);
        if (item.getType() == LauncherAppsDisplayItem.TYPE_QUIZ && item.getQuizGame() != null) {
            bindQuizDisplaySettings(appHolder, item.getQuizGame());
            setAdLabelVisible(appHolder, true);
        } else if (item.getType() == LauncherAppsDisplayItem.TYPE_APP && item.getApp() != null) {
            bindDisplaySettings(appHolder, item.getApp());
            setAdLabelVisible(appHolder, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof NativeSpacerViewHolder) {
            bindNativeSpacer((NativeSpacerViewHolder) holder);
            return;
        }
        if (!(holder instanceof ViewHolder)) {
            return;
        }
        ViewHolder appHolder = (ViewHolder) holder;
        LauncherAppsDisplayItem item = displayItems.get(position);
        if (item.getType() == LauncherAppsDisplayItem.TYPE_NATIVE) {
            return;
        }
        if (item.getType() == LauncherAppsDisplayItem.TYPE_QUIZ) {
            QuizGameItem quizGame = item.getQuizGame();
            if (quizGame == null) {
                return;
            }
            bindQuizDisplaySettings(appHolder, quizGame);
            appHolder.tvAppName.setText(quizGame.getTitle());
            setAdLabelVisible(appHolder, true);
            appHolder.itemView.setClickable(true);
            appHolder.itemView.setOnClickListener(v -> {
                int adapterPosition = appHolder.getBindingAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= displayItems.size()) {
                    return;
                }
                LauncherAppsDisplayItem clicked = displayItems.get(adapterPosition);
                if (clicked.getType() != LauncherAppsDisplayItem.TYPE_QUIZ || clicked.getQuizGame() == null) {
                    return;
                }
                openQuizGame(clicked.getQuizGame().getUrl());
            });
            appHolder.itemView.setOnLongClickListener(null);
            return;
        }

        LauncherAppsModel launcherAppsModel = item.getApp();
        if (launcherAppsModel == null) {
            return;
        }
        bindDisplaySettings(appHolder, launcherAppsModel);
        appHolder.tvAppName.setText(resolveDisplayAppName(launcherAppsModel));
        setAdLabelVisible(appHolder, false);
        appHolder.itemView.setClickable(true);
        appHolder.itemView.setOnClickListener(v -> {
            int adapterPosition = appHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= displayItems.size()) {
                return;
            }
            LauncherAppsDisplayItem clicked = displayItems.get(adapterPosition);
            if (clicked.getType() != LauncherAppsDisplayItem.TYPE_APP || clicked.getApp() == null) {
                return;
            }
            String packageName = clicked.getApp().getPackageName();
            if (context instanceof Activity) {
                LauncherAppsBottomSheet.markSuppressBackgroundDismiss();
                AdPlacement.handleLauncherAppClickAd((Activity) context, () -> launchApp(packageName));
            } else {
                launchApp(packageName);
            }
        });
        appHolder.itemView.setOnLongClickListener(v -> {
            if (longClickListener == null) {
                return false;
            }
            int adapterPosition = appHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= displayItems.size()) {
                return false;
            }
            LauncherAppsDisplayItem clicked = displayItems.get(adapterPosition);
            if (clicked.getType() != LauncherAppsDisplayItem.TYPE_APP || clicked.getApp() == null) {
                return false;
            }
            longClickListener.onLauncherAppLongClick(clicked.getApp(), adapterPosition, v);
            return true;
        });
    }

    public int getSpanSize(int position) {
        if (position < 0 || position >= displayItems.size()) {
            return 1;
        }
        return displayItems.get(position).getType() == LauncherAppsDisplayItem.TYPE_NATIVE ? 4 : 1;
    }

    public int findNativePlaceholderPosition() {
        for (int i = 0; i < displayItems.size(); i++) {
            if (displayItems.get(i).getType() == LauncherAppsDisplayItem.TYPE_NATIVE) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public void setListNativePlaceholderEnabled(boolean enabled) {
        if (listNativePlaceholderEnabled == enabled) {
            return;
        }
        listNativePlaceholderEnabled = enabled;
        if (!enabled) {
            listNativeShimmerVisible = false;
        }
        notifyAppsDataChanged();
    }

    public void setListNativeShimmerVisible(boolean visible) {
        if (listNativeShimmerVisible == visible) {
            return;
        }
        listNativeShimmerVisible = visible;
        int position = findNativePlaceholderPosition();
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position);
        }
    }

    private void bindNativeSpacer(@NonNull NativeSpacerViewHolder holder) {
        if (holder.slNativeListItemShimmer == null) {
            return;
        }
        if (listNativeShimmerVisible) {
            holder.slNativeListItemShimmer.setVisibility(VISIBLE);
            holder.slNativeListItemShimmer.startShimmer();
        } else {
            holder.slNativeListItemShimmer.stopShimmer();
            holder.slNativeListItemShimmer.setVisibility(GONE);
        }
    }

    private void bindQuizDisplaySettings(@NonNull ViewHolder holder, @NonNull QuizGameItem quizGame) {
        if (holder.boundIconSizePx != iconSizePx) {
            ViewGroup.LayoutParams iconParams = holder.ivAppIcon.getLayoutParams();
            iconParams.width = iconSizePx;
            iconParams.height = iconSizePx;
            holder.ivAppIcon.setLayoutParams(iconParams);
            holder.boundIconSizePx = iconSizePx;
        }
        if (holder.boundQuizIconRes != quizGame.getIconRes()) {
            holder.ivAppIcon.setImageResource(quizGame.getIconRes());
            holder.boundQuizIconRes = quizGame.getIconRes();
        }
        if (holder.boundLabelSizeSp != labelSizeSp) {
            holder.tvAppName.setTextSize(labelSizeSp);
            holder.boundLabelSizeSp = labelSizeSp;
        }
        int labelVisibility = labelVisible ? VISIBLE : GONE;
        if (holder.tvAppName.getVisibility() != labelVisibility) {
            holder.tvAppName.setVisibility(labelVisibility);
        }
    }

    private void setAdLabelVisible(@NonNull ViewHolder holder, boolean visible) {
        if (holder.tvAdLabel == null) {
            return;
        }
        int visibility = visible ? VISIBLE : GONE;
        if (holder.tvAdLabel.getVisibility() != visibility) {
            holder.tvAdLabel.setVisibility(visibility);
        }
    }

    private void bindDisplaySettings(@NonNull ViewHolder holder, @NonNull LauncherAppsModel launcherAppsModel) {
        holder.boundQuizIconRes = 0;
        String packageName = launcherAppsModel.getPackageName();

        if (holder.boundIconSizePx != iconSizePx) {
            ViewGroup.LayoutParams iconParams = holder.ivAppIcon.getLayoutParams();
            iconParams.width = iconSizePx;
            iconParams.height = iconSizePx;
            holder.ivAppIcon.setLayoutParams(iconParams);
            holder.boundIconSizePx = iconSizePx;
        }

        Drawable displayIcon = packageName == null ? null : LauncherAppsIconCache.get(packageName, iconSizePx);
        if (displayIcon == null) {
            displayIcon = launcherAppsModel.getAppIcon();
        }
        if (displayIcon != null) {
            if (holder.ivAppIcon.getDrawable() != displayIcon) {
                holder.ivAppIcon.setImageDrawable(displayIcon);
            }
        } else {
            holder.ivAppIcon.setImageResource(R.mipmap.ic_launcher);
        }

        if (holder.boundLabelSizeSp != labelSizeSp) {
            holder.tvAppName.setTextSize(labelSizeSp);
            holder.boundLabelSizeSp = labelSizeSp;
        }
        int labelVisibility = labelVisible ? VISIBLE : GONE;
        if (holder.tvAppName.getVisibility() != labelVisibility) {
            holder.tvAppName.setVisibility(labelVisibility);
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public void setQuizIconsVisible(boolean visible) {
        if (!injectQuizIcons || quizIconsVisible == visible) {
            return;
        }
        quizIconsVisible = visible;
        notifyAppsDataChanged();
    }

    public boolean injectsQuizIcons() {
        return injectQuizIcons;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void notifyAppsDataChanged() {
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivAppIcon;
        private final AppCompatTextView tvAppName;
        private final AppCompatTextView tvAdLabel;
        int boundIconSizePx = -1;
        int boundLabelSizeSp = -1;
        int boundQuizIconRes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvAdLabel = itemView.findViewById(R.id.tvAdLabel);
        }
    }

    private static final class NativeSpacerViewHolder extends RecyclerView.ViewHolder {
        @Nullable
        final com.facebook.shimmer.ShimmerFrameLayout slNativeListItemShimmer;

        NativeSpacerViewHolder(@NonNull View itemView) {
            super(itemView);
            slNativeListItemShimmer = itemView.findViewById(R.id.slNativeListItemShimmer);
        }
    }

    public void applyDisplaySettings() {
        refreshSizeVisibility();
        if (getItemCount() == 0) {
            return;
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_DISPLAY);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateSizeVisibility() {
        applyDisplaySettings();
    }

    public void notifyOrderChanged(@NonNull List<LauncherAppsModel> previousOrder) {
        if (injectQuizIcons) {
            notifyAppsDataChanged();
            return;
        }
        if (items == null || items.isEmpty()) {
            if (previousOrder == null || previousOrder.isEmpty()) {
                return;
            }
            notifyAppsDataChanged();
            return;
        }
        List<LauncherAppsModel> oldList = previousOrder == null ? new ArrayList<>() : previousOrder;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return TextUtils.equals(oldList.get(oldItemPosition).getPackageName(), items.get(newItemPosition).getPackageName());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return true;
            }
        });
        rebuildDisplayItems();
        diffResult.dispatchUpdatesTo(this);
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        if (items == null || items.isEmpty()) {
            return;
        }
        boolean shouldInjectQuiz = injectQuizIcons && quizIconsVisible && AdPlacement.getLauncherAppQuizIconShow();
        if (!shouldInjectQuiz) {
            for (LauncherAppsModel model : items) {
                displayItems.add(LauncherAppsDisplayItem.app(model));
            }
        } else {
            int maxQuizItems = Math.min(MAX_QUIZ_ITEMS, LauncherQuizGames.size());
            int quizLimit = Math.min(Math.max(0, AdPlacement.getLauncherAppQuizIconCount()), maxQuizItems);
            int quizIndex = 0;
            for (LauncherAppsModel model : items) {
                displayItems.add(LauncherAppsDisplayItem.app(model));
                if (quizIndex < quizLimit) {
                    displayItems.add(LauncherAppsDisplayItem.quiz(LauncherQuizGames.get(quizIndex)));
                    quizIndex++;
                }
            }
        }
        if (listNativePlaceholderEnabled && displayItems.size() >= LIST_NATIVE_AFTER_ITEMS) {
            displayItems.add(LIST_NATIVE_AFTER_ITEMS, LauncherAppsDisplayItem.nativeAd());
        }
    }

    private void refreshSizeVisibility() {
        iconSizePx = AppUtils.dpToPx(context, LauncherSettingsHelper.getAppIconSize(context));
        labelSizeSp = LauncherSettingsHelper.getAppLabelSize(context);
        labelVisible = LauncherSettingsHelper.getLabelVisibility(context);
    }

    private void openQuizGame(@NonNull String url) {
        LauncherAppsBottomSheet.markSuppressBackgroundDismiss();
        if (context instanceof Activity) {
            AdPlacement.openQuizGameUrl((Activity) context, url);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void launchApp(String packageName) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                LauncherAppsBottomSheet.markSuppressBackgroundDismiss();
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                AdPlacement.markLauncherExternalAppLaunched(context, packageName);
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private String resolveDisplayAppName(@NonNull LauncherAppsModel model) {
        String packageName = model.getPackageName();
        if (packageName != null) {
            packageName = packageName.trim();
        }
        if (!TextUtils.isEmpty(packageName)) {
            try {
                ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(packageName, 0);
                String label = AppUtils.getApplicationLabelEnglish(context, applicationInfo);
                String cleanedLabel = stripLeadingHash(label);
                if (!cleanedLabel.isEmpty()) {
                    return cleanedLabel;
                }
            } catch (Exception ignored) {
            }
        }

        String cleanedAppName = stripLeadingHash(model.getAppName());
        if (!cleanedAppName.isEmpty()) {
            return cleanedAppName;
        }

        if (!TextUtils.isEmpty(packageName)) {
            return packageName;
        }
        return "App";
    }

    @NonNull
    private static String stripLeadingHash(@Nullable String appName) {
        if (appName == null) {
            return "";
        }
        appName = appName.trim();
        if (appName.startsWith("#")) {
            appName = appName.substring(1).trim();
        }
        return appName;
    }
}