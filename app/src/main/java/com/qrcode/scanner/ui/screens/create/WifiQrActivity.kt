package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.theme.TextPrimary
import com.qrcode.scanner.ui.theme.TextSecondary
import com.qrcode.scanner.ui.theme.TextTertiary
import com.qrcode.scanner.ui.theme.White

/**
 * Dedicated Wi-Fi Create form.
 * Stitch: Create Wi-Fi QR Form (White Theme) cd2a1970b6c44f229e03269e6515505a
 * CTA opens QR Preview (customization deferred).
 */
class WifiQrActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QRCodeScannerTheme {
                WifiQrFormScreen(
                    onBack = { finish() },
                    onSavedClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun WifiQrFormScreen(
    onBack: () -> Unit,
    onSavedClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var security by remember { mutableStateOf(QrPayloadBuilder.WifiSecurity.WPA) }
    var hidden by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ecc by remember { mutableStateOf(QrBitmapEncoder.EccLevel.H) }

    val previewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onSavedClose()
    }

    fun attemptGenerate() {
        val input = QrPayloadBuilder.FormInput(
            primary = ssid,
            secondary = password,
            wifiSecurity = security,
            wifiHidden = hidden
        )
        val validation = QrPayloadBuilder.validate(QrCategoryType.WIFI, input)
        if (validation != null) {
            error = validation
            return
        }
        error = null
        val built = QrPayloadBuilder.build(QrCategoryType.WIFI, input)
        previewLauncher.launch(
            CreateQrIntents.openPreview(
                context = context,
                category = QrCategoryType.WIFI,
                payload = built.payload,
                displayTitle = built.displayTitle,
                detectedType = built.detectedType,
                eccLevel = ecc.name
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FormTopBar(title = "Wi-Fi Network", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live summary chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(999.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Wifi,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = ssid.ifBlank { "Enter SSID" },
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = security.label.uppercase(),
                    color = CobaltPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NestedSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "NETWORK NAME (SSID)",
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )
                FormTextField(
                    label = "",
                    value = ssid,
                    onValueChange = {
                        ssid = it
                        if (error != null) error = null
                    },
                    placeholder = "Enter Wi-Fi SSID",
                    onClear = { ssid = "" }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NestedSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SECURITY TYPE",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(White)
                            .border(1.dp, CobaltSoft, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Recommended: WPA/WPA2",
                            color = CobaltPrimary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(White)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    QrPayloadBuilder.WifiSecurity.entries.forEach { option ->
                        val selected = security == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) CobaltPrimary else White)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    security = option
                                    if (option == QrPayloadBuilder.WifiSecurity.OPEN) {
                                        password = ""
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option.label,
                                color = if (selected) White else TextSecondary,
                                fontFamily = PlusJakartaSans,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (security != QrPayloadBuilder.WifiSecurity.OPEN) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NestedSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PASSWORD",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.6.sp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (error != null) error = null
                            },
                            singleLine = true,
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            cursorBrush = SolidColor(CobaltPrimary),
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontFamily = PlusJakartaSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (password.isEmpty()) {
                                    Text(
                                        text = "Wi-Fi Password",
                                        color = TextTertiary,
                                        fontFamily = PlusJakartaSans,
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        )
                        Icon(
                            imageVector = if (showPassword) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = "Toggle password",
                            tint = TextTertiary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showPassword = !showPassword }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NestedSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hidden Network",
                        color = TextPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "SSID is not broadcasted publicly",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 26.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (hidden) CobaltPrimary else NestedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(999.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { hidden = !hidden }
                        .padding(3.dp),
                    contentAlignment = if (hidden) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(White)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NestedSurface, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = CobaltPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Guests scan this QR with their camera app to connect without typing the Wi-Fi password.",
                    color = TextSecondary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = Destructive,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }

            // ECC (compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QrBitmapEncoder.EccLevel.entries.forEach { level ->
                    val selected = ecc == level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CobaltPrimary else NestedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { ecc = level }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ECC ${level.label}",
                            color = if (selected) White else TextSecondary,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CobaltPrimary)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { attemptGenerate() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Generate & Preview",
                        color = White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
