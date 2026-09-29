# R8 keep rules for release builds.
# Default Android optimize rules are included via optimization.keepRules.includeDefault.
# Manifest components (activities, services, receivers, providers),
# custom views and XML-referenced classes are kept automatically by AAPT-generated rules.
# Firebase, Play Services Ads, ML Kit, CameraX, Gson, Lottie and Install Referrer ship their own consumer rules.

# ---- Crash reports (Firebase Crashlytics) ----
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# ---- Generic signatures / annotations needed by Gson TypeToken and SDK reflection ----
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ---- Gson models ----
# Field names are the JSON keys persisted in SharedPreferences, so they must not be renamed.
-keep class com.qrcode.scanner.launcher.models.ReminderModel { <init>(...); <fields>; }

# ---- Parcelable ----
-keepclassmembers class com.qrcode.scanner.launcher.** implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---- Meta Audience Network mediation (adapter is instantiated by class name) ----
-keep class com.google.ads.mediation.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
