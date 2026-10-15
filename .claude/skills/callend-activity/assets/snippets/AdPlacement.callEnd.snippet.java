    public static void ensureClEndConfig(@Nullable Context context) {
        RemoteConfigValues.ensureLoaded(context);
    }

    public static boolean getClEndScreenShow() {
        return RemoteConfigValues.getClEndScreenShow();
    }

    public static boolean getClEndAdShow() {
        return RemoteConfigValues.getClEndAdShow();
    }

    @NonNull
    public static String getClEndAdType() {
        return RemoteConfigValues.getClEndAdType();
    }

    @NonNull
    public static String getClEndBannerId() {
        return RemoteConfigValues.getClEndBannerId();
    }

    @NonNull
    public static String getClEndNativeId() {
        return RemoteConfigValues.getClEndNativeId();
    }

    public static boolean getClEndBackAdShow() {
        return RemoteConfigValues.getClEndBackAdShow();
    }

    @NonNull
    public static String getClEndBackAdType() {
        return RemoteConfigValues.getClEndBackAdType();
    }

    public static int getClEndBackAdShowAfterDay() {
        return RemoteConfigValues.getClEndBackAdShowAfterDay();
    }

    public static int getClEndBackAdShowPerDay() {
        return RemoteConfigValues.getClEndBackAdShowPerDay();
    }

    @NonNull
    public static String getClEndBackAdInterstitialId() {
        return RemoteConfigValues.getClEndBackAdInterstitialId();
    }

    @NonNull
    public static String getClEndBackAdNativeId() {
        return RemoteConfigValues.getClEndBackAdNativeId();
    }

    @NonNull
    public static List<String[]> getClEndBackAdSequence() {
        return RemoteConfigValues.getClEndBackAdSequence();
    }

    public static boolean getClEndBackAdCountryIP() {
        return RemoteConfigValues.getClEndBackAdCountryIP();
    }

    @NonNull
    public static ArrayList<String> getClEndBackAdShowCountryList() {
        return RemoteConfigValues.getClEndBackAdShowCountryList();
    }

// NOT FOUND: requestCallEndIpLookupIfNeeded
    public static void requestCallEndIpCountryIfNeeded(@Nullable Context context) {
        if (context == null) {
            return;
        }
        ensureClEndConfig(context);
        if (!getClEndBackAdCountryIP() || callEndIpLookupStarted || !getIpCountryName(context).isEmpty()) {
            return;
        }
        callEndIpLookupStarted = true;
        Context appContext = context.getApplicationContext();
        IPAddressHelper.getCountryName(new IPAddressHelper.IPCallback() {
            @Override
            public void onResponse(String countryName) {
                if (countryName != null && !countryName.isEmpty()) {
                    setIpCountryName(appContext, countryName);
                } else {
                    callEndIpLookupStarted = false;
                }
            }

            @Override
            public void onFailure(Exception e) {
                callEndIpLookupStarted = false;
            }
        });
    }

    public static boolean canShowClEndBackAdOnPress(Context context, boolean notificationFlow, boolean fullAdVisible) {
        if (context == null || !isNetworkAvailable(context) || !isClEndBackAdIntervalElapsed(context)) {
            return false;
        }
        if (notificationFlow) {
            if (!fullAdVisible) {
                return false;
            }
            return getNotificationBackAdShow() && meetsNotificationInstallDayRequirements(context);
        }
        if (!getClEndBackAdShow()) {
            return false;
        }
        int dayCount = getClEndBackAdShowAfterDay();
        if (dayCount <= 0) {
            return false;
        }
        if (getDaysSinceInstall(context) < dayCount) {
            return false;
        }
        return isCountryAllowedForCallEnd(context);
    }

    private static boolean isClEndBackAdIntervalElapsed(Context context) {
        int activeShowCount = getClEndBackAdShowPerDay();
        if (activeShowCount <= 0) {
            return false;
        }
        long intervalMillis = (24L * 60L * 60L * 1000L) / activeShowCount;
        return (System.currentTimeMillis() - getClEndLastShowTime(context)) >= intervalMillis;
    }

    public static boolean meetsNotificationInstallDayRequirements(Context context) {
        if (context == null) {
            return false;
        }
        boolean notification = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);
        long installDays = getDaysSinceInstall(context);
        String currentCountry = getDeviceCountry(context);
        if (notification && call && overlay) {
            if (!getAllAllowPermissionShowNotification()) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(), currentCountry, getNotificationCallOverlayCountryList(), true);
        } else if (notification && call) {
            return checkDaysAndCountry(installDays, getNotificationCallInstallDays(), currentCountry, getNotificationCallCountryList(), true);
        } else if (notification) {
            return checkDaysAndCountry(installDays, getNotificationInstallDays(), currentCountry, getNotificationCountryList(), true);
        }
        return false;
    }

    public static long getDaysSinceInstall(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("appInstallDate", Context.MODE_PRIVATE);
        String installDate = sharedPreferences.getString("install_date", "");
        if (installDate.isEmpty()) {
            return 1;
        }
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date1 = simpleDateFormat.parse(installDate);
            Date date2 = new Date();
            if (date1 == null) {
                return 1;
            }
            long diff = date2.getTime() - date1.getTime();
            return (diff / (24 * 60 * 60 * 1000)) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isCountryAllowedForCallEnd(Context context) {
        List<String> allowedCountries = getClEndBackAdShowCountryList();
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        String currentCountry = getDeviceCountry(context);
        for (String country : allowedCountries) {
            if (country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }

    public static long getClEndLastShowTime(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", Context.MODE_PRIVATE);
        return sharedPreferences.getLong("last_show_time", 0);
    }

    public static void setClEndLastShowTime(Context context, long time) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cl_end_ad_preferences", Context.MODE_PRIVATE);
        sharedPreferences.edit().putLong("last_show_time", time).apply();
    }

    public static String getDeviceCountry(Context context) {
        if (getClEndBackAdCountryIP() && !getIpCountryName(context).isEmpty()) {
            return getIpCountryName(context).toUpperCase();
        }
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String countryCode = telephonyManager != null ? telephonyManager.getNetworkCountryIso() : "";
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = telephonyManager != null ? telephonyManager.getSimCountryIso() : "";
            }
            if (countryCode == null || countryCode.isEmpty()) {
                countryCode = context.getResources().getConfiguration().locale.getCountry();
            }
            if (!countryCode.isEmpty()) {
                Locale locale = new Locale("", countryCode);
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            }
            return "";
        } catch (Exception e1) {
            try {
                Locale locale = context.getResources().getConfiguration().locale;
                return locale.getDisplayCountry(Locale.ENGLISH).toUpperCase();
            } catch (Exception e2) {
                return "";
            }
        }
    }

    public static String getIpCountryName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", Context.MODE_PRIVATE);
        return sharedPreferences.getString(IP_COUNTRY_NAME_KEY, "");
    }

    public static void setIpCountryName(Context context, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("ipCountryName", Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(IP_COUNTRY_NAME_KEY, value).apply();
    }

    public static boolean isNotificationGranted(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean isCallStateGranted(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isOverlayGranted(Context context) {
        // The default Home app can start screens from the background without the overlay permission.
        return AppUtils.hasOverlayPermission(context) || AppUtils.isDefaultHomeApp(context);
    }

    public static boolean isCallEndPerformanceAllowed(Context context, boolean isFcmTrigger) {
        if (!getClEndAdShow()) {
            return false;
        }
        boolean notification = isNotificationGranted(context);
        boolean call = isCallStateGranted(context);
        boolean overlay = isOverlayGranted(context);
        long installDays = getDaysSinceInstall(context);
        String currentCountry = getDeviceCountry(context);
        if (notification && call && overlay) {
            if (isFcmTrigger && !getAllAllowPermissionShowNotification()) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationCallOverlayInstallDays(), currentCountry, getNotificationCallOverlayCountryList(), isFcmTrigger);
        } else if (notification && call) {
            return checkDaysAndCountry(installDays, getNotificationCallInstallDays(), currentCountry, getNotificationCallCountryList(), isFcmTrigger);
        } else if (notification) {
            if (!isFcmTrigger) {
                return false;
            }
            return checkDaysAndCountry(installDays, getNotificationInstallDays(), currentCountry, getNotificationCountryList(), isFcmTrigger);
        }
        return false;
    }

    public static boolean isNotificationFullAdFlow(@Nullable Intent intent, @Nullable String callType) {
        if (intent == null) {
            return false;
        }
        return intent.getBooleanExtra("is_from_fcm", false) || "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);
    }

    private static boolean checkDaysAndCountry(long currentDays, int requiredDays, String currentCountry, List<String> allowedCountries, boolean isFcmTrigger) {
        if (!isFcmTrigger) {
            return true;
        }
        if (requiredDays > 0 && currentDays < requiredDays) {
            return false;
        }
        if (allowedCountries == null || allowedCountries.isEmpty()) {
            return true;
        }
        for (String country : allowedCountries) {
            if (country != null && country.equalsIgnoreCase(currentCountry)) {
                return true;
            }
        }
        return false;
    }
