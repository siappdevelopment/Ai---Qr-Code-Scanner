package com.qrcode.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.qrcode.scanner.ui.navigation.AppDestination
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.InactiveNav
import com.qrcode.scanner.ui.theme.White

/**
 * Shared root BottomNavigation — SINGLE component for all root tabs.
 *
 * Stitch source of truth (get_screen + HTML):
 * "History Empty State (White Theme)"
 * ID: e63b148524174fd1ab5cd0ea369e5e1e
 *
 * HTML (`data-purpose="unified-bottom-navigation"`):
 * ```
 * nav: w-full bg-white border-t border-slate-200 (#E2E8F0)
 *      px-4 (16dp) pt-2 (8dp) pb-6 (24dp) flex justify-around items-center
 * tab: w-14 (56dp) py-1 (4dp) flex-col items-center
 * icon: w-5 h-5 (20dp) mb-1 (4dp); active History stroke 2.3
 * label: text-[10px] tracking-tight; inactive font-medium slate-500 (#64748B);
 *        active font-bold #0033CC
 * History dot: absolute -top-0.5 -right-1 w-1.5 h-1.5 (6dp) bg #0033CC
 * Scan FAB wrapper: relative -top-5 (-20dp)
 * Scan FAB: w-14 h-14 (56dp) rounded-full bg #0033CC border-2 border-white
 * Scan icon: w-7 h-7 (28dp) stroke 2.2
 * Scan label: text-[10px] font-bold text #0033CC mt-1 (4dp)
 * ```
 * No shadows / elevation / gradients.
 */
@Composable
fun ScanPulseBottomBar(
    currentRoute: String?,
    onNavigate: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    // Stitch FAB uses relative -top-5 and paints above the nav surface.
    // Transparent overhang (not white) so the FAB floats over page content
    // instead of sitting inside an expanded white slab (previous bug).
    val fabOverhang = 20.dp // -top-5

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Transparent space so FAB can draw above the white nav without clipping
        Spacer(modifier = Modifier.fillMaxWidth().height(fabOverhang))

        // White nav surface — matches Stitch <nav> box (starts at border-t)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
        ) {
            // border-t border-slate-200
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFE2E8F0))
            )

            // px-4 pt-2 pb-6 + justify-around items-center
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTab(
                        label = "Home",
                        selected = currentRoute == AppDestination.Home.route,
                        icon = ScanPulseIcons.Home,
                        onClick = { onNavigate(AppDestination.Home) }
                    )
                    NavTab(
                        label = "Create",
                        selected = currentRoute == AppDestination.Create.route,
                        icon = ScanPulseIcons.Create,
                        onClick = { onNavigate(AppDestination.Create) }
                    )

                    // Center Scan — layout stays in-row; visual -top-5 into overhang
                    ScanFabTab(
                        selected = currentRoute == AppDestination.Scan.route,
                        overhang = fabOverhang,
                        onClick = { onNavigate(AppDestination.Scan) }
                    )

                    NavTab(
                        label = "History",
                        selected = currentRoute == AppDestination.History.route,
                        icon = if (currentRoute == AppDestination.History.route) {
                            ScanPulseIcons.HistoryActive
                        } else {
                            ScanPulseIcons.History
                        },
                        showActiveDot = currentRoute == AppDestination.History.route,
                        onClick = { onNavigate(AppDestination.History) }
                    )
                    NavTab(
                        label = "Settings",
                        selected = currentRoute == AppDestination.Settings.route,
                        icon = ScanPulseIcons.Settings,
                        onClick = { onNavigate(AppDestination.Settings) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTab(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    showActiveDot: Boolean = false
) {
    val color = if (selected) CobaltPrimary else InactiveNav
    // Stitch: inactive font-medium; active font-bold
    val weight = if (selected) FontWeight.Bold else FontWeight.Medium

    Column(
        modifier = Modifier
            .width(56.dp) // w-14
            .padding(vertical = 4.dp) // py-1
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp) // w-5 h-5
            )
            if (showActiveDot) {
                // absolute -top-0.5 (-2dp) -right-1 (-4dp) → from top-end: x=+4dp, y=-2dp
                // w-1.5 h-1.5 = 6dp
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .size(6.dp)
                        .background(CobaltPrimary, CircleShape)
                )
            }
        }
        // mb-1 on icon ⇒ 4dp gap before label; tracking-tight
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = weight,
            letterSpacing = (-0.1).sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ScanFabTab(
    @Suppress("UNUSED_PARAMETER") selected: Boolean,
    overhang: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .zIndex(1f)
            .offset(y = -overhang) // relative -top-5
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // w-14 h-14 rounded-full bg-[#0033CC] border-2 border-white
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(CobaltPrimary, CircleShape)
                .border(width = 2.dp, color = White, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ScanPulseIcons.ScanReticle,
                contentDescription = "Quick Scan",
                tint = White,
                modifier = Modifier.size(28.dp) // w-7 h-7
            )
        }
        // mt-1 text-[10px] font-bold text-[#0033CC] tracking-tight
        Text(
            text = "Scan",
            color = CobaltPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.1).sp,
            lineHeight = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Icons matching Stitch SVG paths from the unified BottomNavigation.
 */
object ScanPulseIcons {
    val Home: ImageVector by lazy {
        strokeIcon("Home", strokeWidth = 2f) {
            moveTo(2.25f, 12f)
            lineToRelative(8.954f, -8.955f)
            curveToRelative(0.44f, -0.439f, 1.152f, -0.439f, 1.591f, 0f)
            lineTo(21.75f, 12f)
            moveTo(4.5f, 9.75f)
            verticalLineToRelative(10.125f)
            curveToRelative(0f, 0.621f, 0.504f, 1.125f, 1.125f, 1.125f)
            horizontalLineTo(9.75f)
            verticalLineToRelative(-4.875f)
            curveToRelative(0f, -0.621f, 0.504f, -1.125f, 1.125f, -1.125f)
            horizontalLineToRelative(2.25f)
            curveToRelative(0.621f, 0f, 1.125f, 0.504f, 1.125f, 1.125f)
            verticalLineTo(21f)
            horizontalLineToRelative(4.125f)
            curveToRelative(0.621f, 0f, 1.125f, -0.504f, 1.125f, -1.125f)
            verticalLineTo(9.75f)
            moveTo(8.25f, 21f)
            horizontalLineToRelative(8.25f)
        }
    }

    val Create: ImageVector by lazy {
        strokeIcon("Create", strokeWidth = 2f) {
            moveTo(12f, 4.5f)
            verticalLineToRelative(15f)
            moveToRelative(7.5f, -7.5f)
            horizontalLineToRelative(-15f)
        }
    }

    val History: ImageVector by lazy {
        historyIcon(strokeWidth = 2f)
    }

    /** Stitch active History uses stroke-width="2.3". */
    val HistoryActive: ImageVector by lazy {
        historyIcon(strokeWidth = 2.3f)
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(9.594f, 3.94f)
                curveToRelative(0.09f, -0.542f, 0.56f, -0.94f, 1.11f, -0.94f)
                horizontalLineToRelative(2.593f)
                curveToRelative(0.55f, 0f, 1.02f, 0.398f, 1.11f, 0.94f)
                lineToRelative(0.213f, 1.281f)
                curveToRelative(0.063f, 0.374f, 0.313f, 0.686f, 0.645f, 0.87f)
                curveToRelative(0.074f, 0.04f, 0.147f, 0.083f, 0.22f, 0.127f)
                curveToRelative(0.325f, 0.196f, 0.72f, 0.257f, 1.075f, 0.124f)
                lineToRelative(1.217f, -0.456f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, 1.37f, 0.49f)
                lineToRelative(1.296f, 2.247f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, -0.26f, 1.431f)
                lineToRelative(-1.003f, 0.827f)
                curveToRelative(-0.293f, 0.241f, -0.438f, 0.613f, -0.43f, 0.992f)
                arcToRelative(7.723f, 7.723f, 0f, false, true, 0f, 0.255f)
                curveToRelative(-0.008f, 0.378f, 0.137f, 0.75f, 0.43f, 0.991f)
                lineToRelative(1.004f, 0.827f)
                curveToRelative(0.424f, 0.35f, 0.534f, 0.955f, 0.26f, 1.43f)
                lineToRelative(-1.298f, 2.247f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, -1.369f, 0.491f)
                lineToRelative(-1.217f, -0.456f)
                curveToRelative(-0.355f, -0.133f, -0.75f, -0.072f, -1.076f, 0.124f)
                arcToRelative(6.6f, 6.6f, 0f, false, true, -0.22f, 0.128f)
                curveToRelative(-0.331f, 0.183f, -0.581f, 0.495f, -0.644f, 0.869f)
                lineToRelative(-0.213f, 1.281f)
                curveToRelative(-0.09f, 0.543f, -0.56f, 0.94f, -1.11f, 0.94f)
                horizontalLineToRelative(-2.594f)
                curveToRelative(-0.55f, 0f, -1.019f, -0.398f, -1.11f, -0.94f)
                lineToRelative(-0.213f, -1.281f)
                curveToRelative(-0.062f, -0.374f, -0.312f, -0.686f, -0.644f, -0.87f)
                arcToRelative(6.52f, 6.52f, 0f, false, true, -0.22f, -0.127f)
                curveToRelative(-0.325f, -0.196f, -0.72f, -0.257f, -1.076f, -0.124f)
                lineToRelative(-1.217f, 0.456f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, -1.369f, -0.49f)
                lineToRelative(-1.297f, -2.247f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, 0.26f, -1.431f)
                lineToRelative(1.004f, -0.827f)
                curveToRelative(0.292f, -0.24f, 0.437f, -0.613f, 0.43f, -0.991f)
                arcToRelative(6.932f, 6.932f, 0f, false, true, 0f, -0.255f)
                curveToRelative(0.007f, -0.38f, -0.138f, -0.751f, -0.43f, -0.992f)
                lineToRelative(-1.004f, -0.827f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, -0.26f, -1.43f)
                lineToRelative(1.297f, -2.247f)
                arcToRelative(1.125f, 1.125f, 0f, false, true, 1.37f, -0.491f)
                lineToRelative(1.216f, 0.456f)
                curveToRelative(0.356f, 0.133f, 0.751f, 0.072f, 1.076f, -0.124f)
                curveToRelative(0.072f, -0.044f, 0.146f, -0.086f, 0.22f, -0.128f)
                curveToRelative(0.332f, -0.183f, 0.582f, -0.495f, 0.644f, -0.869f)
                lineToRelative(0.214f, -1.28f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(15f, 12f)
                arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
                arcToRelative(3f, 3f, 0f, false, true, 6f, 0f)
                close()
            }
        }.build()
    }

    val ScanReticle: ImageVector by lazy {
        ImageVector.Builder(
            name = "ScanReticle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val w = 2.2f
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(4f, 8f)
                verticalLineTo(6f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                horizontalLineToRelative(2f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(4f, 16f)
                verticalLineToRelative(2f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineToRelative(2f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(16f, 4f)
                horizontalLineToRelative(2f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                verticalLineToRelative(2f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(16f, 20f)
                horizontalLineToRelative(2f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineToRelative(-2f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null
            ) {
                moveTo(7f, 12f)
                horizontalLineToRelative(10f)
            }
        }.build()
    }

    private fun historyIcon(strokeWidth: Float): ImageVector =
        strokeIcon("History_$strokeWidth", strokeWidth) {
            moveTo(12f, 6f)
            verticalLineToRelative(6f)
            horizontalLineToRelative(4.5f)
            moveToRelative(4.5f, 0f)
            arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
            arcToRelative(9f, 9f, 0f, false, true, 18f, 0f)
            close()
        }

    private fun strokeIcon(
        name: String,
        strokeWidth: Float,
        builder: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                pathBuilder = builder
            )
        }.build()
}
