package com.qrcode.scanner.launcher.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.qrcode.scanner.launcher.fragments.LauncherHomeFragment;
import com.qrcode.scanner.launcher.fragments.SubContainerFragment;

/**
 * Home sits between a right-swipe slot and the sub page, matching the source pager indexes.
 * The right-swipe slot is not the source second-app UI. Selecting it opens Compose MainActivity.
 */
public class LauncherPagerAdapter extends FragmentStateAdapter {
    public static final int PAGE_COUNT = 3;
    public static final int PAGE_RIGHT = 0;
    public static final int PAGE_HOME = 1;
    public static final int PAGE_SUB = 2;

    public LauncherPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == PAGE_SUB) {
            return new SubContainerFragment();
        }
        if (position == PAGE_RIGHT) {
            return new Fragment();
        }
        return new LauncherHomeFragment();
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < PAGE_COUNT;
    }
}
