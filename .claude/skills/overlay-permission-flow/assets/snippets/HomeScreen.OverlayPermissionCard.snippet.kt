}
/**
 * Shown on Home while the app is neither the default Home app nor allowed to display over other apps.
 * Allow opens the system "Display over other apps" screen for this app. It disappears once either is true;
 * the state is re-read every time Home resumes (coming back from that settings screen).
 */
@Composable
private fun OverlayPermissionCard() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun needsOverlay() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
        !AppUtils.hasOverlayPermission(context) && !AppUtils.isDefaultHomeApp(context)
    var show by remember { mutableStateOf(needsOverlay()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                show = needsOverlay()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (!show) {
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardSurface, RoundedCornerShape(20.dp))
            .border(1.dp, HomeColors.Outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(HomeColors.IconWell, RoundedCornerShape(14.dp))
                .border(1.dp, HomeColors.IconWellBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Layers,
                contentDescription = null,
                tint = CobaltPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_overlay_title),
                color = HomeColors.OnSurface,
                fontFamily = PlusJakartaSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
//            Spacer(modifier = Modifier.height(2.dp))
//            Text(
//                text = stringResource(R.string.home_overlay_description),
//                color = HomeColors.OnSurfaceVariant,
//                fontFamily = PlusJakartaSans,
//                fontSize = 12.sp,
//                lineHeight = 17.sp
//            )
        }
        Box(
            modifier = Modifier
                .height(38.dp)
                .background(CobaltPrimary, RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    try {
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:" + context.packageName)
                            )
                        )
                        com.qrcode.scanner.launcher.activities.OverlayPermissionGuideActivity.show(context)
                    } catch (_: Exception) {
                    }
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_overlay_allow),
                color = White,
                fontFamily = PlusJakartaSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
