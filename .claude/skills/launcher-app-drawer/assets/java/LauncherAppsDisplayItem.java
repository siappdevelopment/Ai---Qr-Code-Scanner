package com.qrcode.scanner.launcher.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LauncherAppsDisplayItem {
    public static final int TYPE_APP = 0;
    public static final int TYPE_QUIZ = 1;
    public static final int TYPE_NATIVE = 2;

    private final int type;
    @Nullable
    private final LauncherAppsModel app;
    @Nullable
    private final QuizGameItem quizGame;

    private LauncherAppsDisplayItem(int type, @Nullable LauncherAppsModel app, @Nullable QuizGameItem quizGame) {
        this.type = type;
        this.app = app;
        this.quizGame = quizGame;
    }

    @NonNull
    public static LauncherAppsDisplayItem app(@NonNull LauncherAppsModel app) {
        return new LauncherAppsDisplayItem(TYPE_APP, app, null);
    }

    @NonNull
    public static LauncherAppsDisplayItem quiz(@NonNull QuizGameItem quizGame) {
        return new LauncherAppsDisplayItem(TYPE_QUIZ, null, quizGame);
    }

    @NonNull
    public static LauncherAppsDisplayItem nativeAd() {
        return new LauncherAppsDisplayItem(TYPE_NATIVE, null, null);
    }

    public int getType() {
        return type;
    }

    @Nullable
    public LauncherAppsModel getApp() {
        return app;
    }

    @Nullable
    public QuizGameItem getQuizGame() {
        return quizGame;
    }
}
