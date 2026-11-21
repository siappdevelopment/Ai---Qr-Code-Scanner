package com.qrcode.scanner.launcher.common;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.qrcode.scanner.launcher.activities.LauncherSettingsActivity;
import com.qrcode.scanner.spapp.R;
import com.qrcode.scanner.ui.screens.create.CreateQrIntents;
import com.qrcode.scanner.ui.screens.history.HistoryIntents;
import com.qrcode.scanner.ui.screens.scan.ScanIntents;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * The content of the Recent screen (header, Clear all, recently used apps strip, Quick actions grid) bound to a root view.
 * The SAME page backs the real {@link com.qrcode.scanner.launcher.activities.RecentActivity} and the instant
 * {@link RecentOverlay}, so the hand-over from overlay to activity is invisible. The inline ad slot belongs to the
 * activity only (the overlay hides it).
 */
public final class RecentPage {
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    /** Apps strip of the last load, so a freshly created page (overlay) already has its icons in the first frame. */
    private static volatile List<AppItem> cache = new ArrayList<>();

    public interface Host {
        void onClose();

        void onOpenApp(@NonNull String packageName);

        void onOpenFeature(@NonNull Intent intent);
    }

    static final class AppItem {
        final String packageName;
        final CharSequence label;
        final Drawable icon;

        AppItem(String packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private static final class Option {
        @StringRes
        final int title;
        @DrawableRes
        final int icon;
        final Function<Context, Intent> intent;

        Option(@StringRes int title, @DrawableRes int icon, Function<Context, Intent> intent) {
            this.title = title;
            this.icon = icon;
            this.intent = intent;
        }
    }

    private static final List<Option> OPTIONS = new ArrayList<>();

    static {
        OPTIONS.add(new Option(R.string.scan_qr, R.drawable.ic_home_action_scan, ScanIntents.INSTANCE::openScanner));
        OPTIONS.add(new Option(R.string.create_qr, R.drawable.ic_home_action_create, CreateQrIntents.INSTANCE::openHub));
        OPTIONS.add(new Option(R.string.history_title, R.drawable.ic_home_action_history, HistoryIntents.INSTANCE::openHistory));
        OPTIONS.add(new Option(R.string.favorites_title, R.drawable.ic_star_filled, HistoryIntents.INSTANCE::openFavorites));
        OPTIONS.add(new Option(R.string.settings_title, R.drawable.ic_home_action_setting, context -> new Intent(context, LauncherSettingsActivity.class)));
    }

    /** Shared launch helpers of the overlay and the activity. */
    public static final class Launch {
        private Launch() {
        }

        /** Starts the launcher underneath and the chosen app on top (closing the app lands on the launcher) and records it as recent. */
        public static boolean app(@NonNull Context context, @NonNull String packageName) {
            try {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent == null) {
                    return false;
                }
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent homeIntent = new Intent(context, com.qrcode.scanner.launcher.activities.LauncherHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivities(new Intent[]{homeIntent, launchIntent});
                AdPlacement.markLauncherExternalAppLaunched(context, packageName);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

    private final View root;
    private final Host host;
    private final Context context;
    private RecyclerView rvApps;
    private AppCompatTextView tvEmpty;
    private AppCompatTextView tvClear;
    private int loadToken;

    public RecentPage(@NonNull View root, @NonNull Host host) {
        this.root = root;
        this.host = host;
        this.context = root.getContext();
    }

    /** Loads the apps strip into the cache ahead of time (called at install) so the overlay's first frame has icons. */
    public static void warm(@NonNull Context context) {
        final Context app = context.getApplicationContext();
        LOADER.execute(() -> cache = loadApps(app));
    }

    public void bind() {
        rvApps = root.findViewById(R.id.rvApps);
        tvEmpty = root.findViewById(R.id.tvEmpty);
        tvClear = root.findViewById(R.id.tvClear);
        RecyclerView rvOptions = root.findViewById(R.id.rvOptions);
        root.findViewById(R.id.ivClose).setOnClickListener(v -> host.onClose());
        tvClear.setOnClickListener(v -> {
            RecentAppsStore.clear(context);
            cache = new ArrayList<>();
            showApps(cache);
        });
        rvOptions.setLayoutManager(new GridLayoutManager(context, 2));
        rvOptions.setAdapter(new OptionsAdapter());
        showApps(cache);
    }

    /** Reloads the apps strip off the main thread; a stale answer is dropped by the token. */
    public void refresh() {
        final int token = ++loadToken;
        final Context app = context.getApplicationContext();
        LOADER.execute(() -> {
            List<AppItem> items = loadApps(app);
            cache = items;
            MAIN.post(() -> {
                if (token == loadToken) {
                    showApps(items);
                }
            });
        });
    }

    private void showApps(List<AppItem> items) {
        if (rvApps == null) {
            return;
        }
        rvApps.setAdapter(new AppsAdapter(items));
        boolean empty = items.isEmpty();
        rvApps.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvClear.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private static List<AppItem> loadApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<AppItem> items = new ArrayList<>();
        for (String packageName : RecentAppsStore.recentApps(context)) {
            try {
                items.add(new AppItem(packageName, pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)), pm.getApplicationIcon(packageName)));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return items;
    }

    private final class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.Holder> {
        final class Holder extends RecyclerView.ViewHolder {
            final AppCompatImageView ivIcon;
            final AppCompatTextView tvTitle;

            Holder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvTitle = itemView.findViewById(R.id.tvTitle);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_recent_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Option option = OPTIONS.get(position);
            holder.ivIcon.setImageResource(option.icon);
            holder.tvTitle.setText(option.title);
            holder.itemView.setOnClickListener(v -> host.onOpenFeature(option.intent.apply(context)));
        }

        @Override
        public int getItemCount() {
            return OPTIONS.size();
        }
    }

    private final class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.Holder> {
        private final List<AppItem> items;

        AppsAdapter(List<AppItem> items) {
            this.items = items;
        }

        final class Holder extends RecyclerView.ViewHolder {
            final AppCompatImageView ivIcon;
            final AppCompatTextView tvName;

            Holder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvName = itemView.findViewById(R.id.tvName);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_recent_app, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AppItem item = items.get(position);
            holder.ivIcon.setImageDrawable(item.icon);
            holder.tvName.setText(item.label);
            holder.itemView.setOnClickListener(v -> host.onOpenApp(item.packageName));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
