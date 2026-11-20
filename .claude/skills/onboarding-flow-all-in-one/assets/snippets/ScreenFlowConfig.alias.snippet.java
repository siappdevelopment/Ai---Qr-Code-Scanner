// In ScreenFlowConfig.normalizeScreenName(): accept the older flow names so a flow copied from another app works.
// (PolicyScreen is NOT an alias in the current code - without this line it is silently skipped.)
if (value.equalsIgnoreCase(SCREEN_COLLECTION) || value.equalsIgnoreCase("PolicyScreen") || value.equalsIgnoreCase("Policy")) {
    return SCREEN_COLLECTION;
}
// "OnBoarding" / "Onboarding" / "IntroScreen" / "IntroActivity" already map to SCREEN_INTRO.

// In RemoteConfigHelper / ScreenFlowConfig.applyShowScreenFlowFromConfig(): the code reads the key "Show_Screen_Flow"
// (nested in the JSON, or the top-level Remote Config parameter). To also accept "show_intro_screen_flow":
//   if (jsonObject.has("show_intro_screen_flow")) nestedFlow = parseShowScreenFlow(jsonObject.opt("show_intro_screen_flow"));
