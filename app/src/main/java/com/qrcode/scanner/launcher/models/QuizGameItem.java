package com.qrcode.scanner.launcher.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class QuizGameItem {
    private final String title;
    private final String url;
    @DrawableRes
    private final int iconRes;

    public QuizGameItem(@NonNull String title, @NonNull String url, @DrawableRes int iconRes) {
        this.title = title;
        this.url = url;
        this.iconRes = iconRes;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }
}
