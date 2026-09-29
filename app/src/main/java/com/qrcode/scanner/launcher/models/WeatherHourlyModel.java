package com.qrcode.scanner.launcher.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class WeatherHourlyModel {
    @NonNull
    public final String timeLabel;
    @DrawableRes
    public final int iconRes;
    @NonNull
    public final String temperature;

    public WeatherHourlyModel(@NonNull String timeLabel, @DrawableRes int iconRes, @NonNull String temperature) {
        this.timeLabel = timeLabel;
        this.iconRes = iconRes;
        this.temperature = temperature;
    }
}
