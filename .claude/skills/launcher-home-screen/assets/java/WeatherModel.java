package com.qrcode.scanner.launcher.models;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeatherModel {
    @NonNull
    public final String location;
    @NonNull
    public final String temperature;
    @NonNull
    public final String condition;
    @NonNull
    public final String highTemperature;
    @NonNull
    public final String lowTemperature;
    @DrawableRes
    public final int iconRes;
    @NonNull
    public final List<WeatherHourlyModel> hourlyForecast;

    public WeatherModel(@NonNull String location, @NonNull String temperature, @NonNull String condition, @NonNull String highTemperature, @NonNull String lowTemperature, @DrawableRes int iconRes, @NonNull List<WeatherHourlyModel> hourlyForecast) {
        this.location = location;
        this.temperature = temperature;
        this.condition = condition;
        this.highTemperature = highTemperature;
        this.lowTemperature = lowTemperature;
        this.iconRes = iconRes;
        this.hourlyForecast = Collections.unmodifiableList(new ArrayList<>(hourlyForecast));
    }
}
