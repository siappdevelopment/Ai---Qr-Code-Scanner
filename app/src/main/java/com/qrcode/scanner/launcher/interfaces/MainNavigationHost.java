package com.qrcode.scanner.launcher.interfaces;

import androidx.annotation.Nullable;

public interface MainNavigationHost {
    void navigateToPage(int page);

    void refreshHistoryFragments();

    int getLastHistoryTab();

    void setLastHistoryTab(int tab);

    @Nullable
    default MainNavigationHost resolveMainNavigationHost() {
        return this;
    }
}
