    public static List<String> getDefaultShowScreenFlow() {
        List<String> defaults = new ArrayList<>();
        defaults.add(SCREEN_LANGUAGE);
        defaults.add(SCREEN_COLLECTION);
        defaults.add(SCREEN_PERMISSION);
        defaults.add(SCREEN_DEFAULT_HOME);
        defaults.add(SCREEN_INTRO);
        return defaults;
    }

    private static String normalizeScreenName(String screen) {
        if (screen == null) {
            return "";
        }
        String value = screen.trim();
        if (value.isEmpty()) {
            return "";
        }
        if (value.equalsIgnoreCase(SCREEN_LANGUAGE)) {
            return SCREEN_LANGUAGE;
        }
        if (value.equalsIgnoreCase(SCREEN_COLLECTION)) {
            return SCREEN_COLLECTION;
        }
        if (value.equalsIgnoreCase(SCREEN_PERMISSION)) {
            return SCREEN_PERMISSION;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_HOME) || value.equalsIgnoreCase("Default") || value.equalsIgnoreCase("DefaultApp") || value.equalsIgnoreCase("Default_Home")) {
            return SCREEN_DEFAULT_HOME;
        }
        if (value.equalsIgnoreCase(SCREEN_DEFAULT_SETTING_HOME) || value.equalsIgnoreCase("DefaultSettingHome")) {
            return SCREEN_DEFAULT_SETTING_HOME;
        }
        if (value.equalsIgnoreCase(SCREEN_INTRO) || value.equalsIgnoreCase("IntroActivity") || value.equalsIgnoreCase("IntroScreen") || value.equalsIgnoreCase("Onboarding")) {
            return SCREEN_INTRO;
        }
        return value;
    }
