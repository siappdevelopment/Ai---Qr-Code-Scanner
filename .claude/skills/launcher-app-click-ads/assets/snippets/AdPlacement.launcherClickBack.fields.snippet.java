// Fields to add to AdPlacement (class level)
    private static final String LAUNCHER_APP_BACK_PREFS = "launcher_app_back_ad";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_INTER = "google_inter";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_APP_OPEN = "google_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_GOOGLE_NATIVE = "google_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_INTER = "quiz_inter";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_APP_OPEN = "quiz_app_open";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_NATIVE = "quiz_native";
    private static final String LAUNCHER_APP_AD_TYPE_QUIZ_BROWSER = "quiz_browser";
    private static boolean launcherAppClickAdShow;
    private static int launcherAppCount;
    private static final ArrayList<String> launcherAppAdSequence = new ArrayList<>();
    private static int launcherAppAdSequenceIndex;
    private static String launcherAppInterstitialId = "";
    private static boolean launcherAppBackClickAdShow;
    private static int launcherAppBackCount;
    private static final ArrayList<String> launcherAppBackAdSequence = new ArrayList<>();
    private static int launcherAppBackAdSequenceIndex;
    private static String launcherAppBackInterstitialId = "";
    private static boolean launcherGoogleAdFailedShowQuiz;
    private static int launcherAppClickCount = 1;
154:    private static boolean launcherGoogleAdFailedShowQuiz;
156:    private static boolean launcherBackConfigRestored;
860:        launcherGoogleAdFailedShowQuiz = value;
888:                .putBoolean("backQuizOnFail", launcherGoogleAdFailedShowQuiz)
