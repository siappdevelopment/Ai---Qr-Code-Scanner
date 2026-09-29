package com.qrcode.scanner.launcher.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.qrcode.scanner.launcher.fragments.ClContentFragment;
import com.qrcode.scanner.launcher.fragments.ClMessageFragment;
import com.qrcode.scanner.launcher.fragments.ClMoreOptionFragment;
import com.qrcode.scanner.launcher.fragments.ClReminderFragment;

public class ClEndPagerAdapter extends FragmentStateAdapter {
    public ClEndPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return new ClMessageFragment();
            case 2:
                return new ClReminderFragment();
            case 3:
                return new ClMoreOptionFragment();
            default:
                return new ClContentFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
