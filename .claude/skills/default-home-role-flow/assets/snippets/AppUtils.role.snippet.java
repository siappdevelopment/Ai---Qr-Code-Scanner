    public static boolean isDefaultHomeApp(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Boolean roleHeld = roleHomeHeldOrNull(context);
            if (roleHeld != null) {
                return roleHeld;
            }
        }
        return isDefaultHomeByResolve(context);
    }

    public static Intent createDefaultHomeRoleRequestIntent(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        return createRoleHomeRequestIntent(context);
    }

    public static boolean openDefaultHomeChooser(Activity activity) {
        if (activity == null) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Boolean roleHomeHeldOrNull(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            return roleManager.isRoleHeld(RoleManager.ROLE_HOME);
        }
        return null;
    }

    private static Intent createRoleHomeRequestIntent(Context context) {
        RoleManager roleManager = context.getSystemService(RoleManager.class);
        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_HOME) || roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            return null;
        }
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME);
    }

    private static boolean isDefaultHomeByResolve(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        return context.getPackageName().equals(resolveInfo.activityInfo.packageName);
    }

    public static boolean hasPhoneStatePermission(Context context) {
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasOverlayPermission(Context context) {
        if (context == null) {
            return false;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Exception e) {
            return false;
        }
    }
