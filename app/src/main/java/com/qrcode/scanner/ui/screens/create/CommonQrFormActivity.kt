package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
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
 * Shared Create form Activity.
 * Stitch shell: Create Website QR Form (White) eeb194fdab9a4407b1d8df102c1da6d2
 * Fields adapt per [QrCategoryType]; BARCODE is rejected (not a QR).
 */
class CommonQrFormActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val type = QrCategoryType.fromIntentExtra(
            intent.getStringExtra(QrCategoryType.EXTRA_QR_CATEGORY)
        )
        setContent {
            QRCodeScannerTheme {
                CommonQrFormScreen(
                    category = type,
                    onBack = { finish() },
                    onSavedClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun CommonQrFormScreen(
    category: QrCategoryType,
    onBack: () -> Unit,
    onSavedClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val labels = remember(category) { FormLabels.forCategory(category) }
    val settingsRepository = remember { SettingsRepositoryProvider.get(context) }
    val settingsPrefs by settingsRepository.preferences.collectAsStateWithLifecycle(
        initialValue = SettingsPreferences()
    )

    var primary by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var tertiary by remember { mutableStateOf("") }
    var quaternary by remember { mutableStateOf("") }
    val ecc = settingsPrefs.defaultQrEcc.toEncoderLevel()
    var error by remember { mutableStateOf<String?>(null) }

    val previewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onSavedClose()
        }
    }

    fun attemptGenerate() {
        val input = QrPayloadBuilder.FormInput(
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            quaternary = quaternary
        )
        val validation = QrPayloadBuilder.validate(category, input)
        if (validation != null) {
            error = validation
            return
        }
        error = null
        val built = QrPayloadBuilder.build(category, input)
        previewLauncher.launch(
            CreateQrIntents.openPreview(
                context = context,
                category = category,
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
        FormTopBar(title = category.displayTitle, onBack = onBack)

        if (category == QrCategoryType.BARCODE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Barcode creation coming later",
                        color = TextPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This hub item is for barcodes (EAN/UPC), not QR codes. QR categories are available from Create.",
                        color = TextSecondary,
                        fontFamily = PlusJakartaSans,
                        fontSize = 14.sp
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CobaltSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.displayTitle,
                    color = CobaltPrimary,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface, RoundedCornerShape(16.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FormTextField(
                    label = labels.primaryLabel,
                    value = primary,
                    onValueChange = {
                        primary = if (BarcodeSymbology.isBarcode(category)) {
                            BarcodeSymbology.filterInput(category, it)
                        } else {
                            it
                        }
                        if (error != null) error = null
                    },
                    placeholder = labels.primaryPlaceholder,
                    keyboardType = if (BarcodeSymbology.isNumericInput(category)) {
                        KeyboardType.Number
                    } else {
                        KeyboardType.Unspecified
                    },
                    trailingPaste = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrBlank()) {
                            primary = if (BarcodeSymbology.isBarcode(category)) {
                                BarcodeSymbology.filterInput(category, text)
                            } else {
                                text
                            }
                            error = null
                        } else {
                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClear = { primary = "" }
                )
                if (labels.secondaryLabel != null) {
                    FormTextField(
                        label = labels.secondaryLabel,
                        value = secondary,
                        onValueChange = { secondary = it },
                        placeholder = labels.secondaryPlaceholder.orEmpty(),
                        optional = labels.secondaryOptional
                    )
                }
                if (labels.quaternaryLabel != null) {
                    FormTextField(
                        label = labels.quaternaryLabel,
                        value = quaternary,
                        onValueChange = { quaternary = it },
                        placeholder = labels.quaternaryPlaceholder.orEmpty(),
                        optional = labels.quaternaryOptional
                    )
                }
                if (labels.tertiaryLabel != null) {
                    FormTextField(
                        label = labels.tertiaryLabel,
                        value = tertiary,
                        onValueChange = { tertiary = it },
                        placeholder = labels.tertiaryPlaceholder.orEmpty(),
                        optional = true
                    )
                }
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                        text = if (BarcodeSymbology.isBarcode(category)) {
                            "Generate & Preview"
                        } else {
                            "Generate & Preview QR"
                        },
                        color = White,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SecondaryFormAction(
                    label = "Instant Share",
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val text = primary.trim()
                        if (text.isEmpty()) {
                            Toast.makeText(context, "Nothing to share yet", Toast.LENGTH_SHORT)
                                .show()
                            return@SecondaryFormAction
                        }
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "Share"))
                    }
                )
            }
        }
    }
}

private data class FormLabels(
    val primaryLabel: String,
    val primaryPlaceholder: String,
    val secondaryLabel: String? = null,
    val secondaryPlaceholder: String? = null,
    val secondaryOptional: Boolean = true,
    val tertiaryLabel: String? = null,
    val tertiaryPlaceholder: String? = null,
    val quaternaryLabel: String? = null,
    val quaternaryPlaceholder: String? = null,
    val quaternaryOptional: Boolean = true
) {
    companion object {
        fun forCategory(type: QrCategoryType): FormLabels = when (type) {
            QrCategoryType.WEBSITE -> FormLabels(
                primaryLabel = "Website URL (https://)",
                primaryPlaceholder = "https://example.com/target-path",
                secondaryLabel = "Display Title / Note",
                secondaryPlaceholder = "e.g., Marketing Deck Q3 Launch"
            )
            QrCategoryType.PLAIN_TEXT -> FormLabels(
                primaryLabel = "Raw Message / Payload",
                primaryPlaceholder = "Type or paste plain text…",
                secondaryLabel = "Display Title / Note",
                secondaryPlaceholder = "Optional label"
            )
            QrCategoryType.CONTACT -> FormLabels(
                primaryLabel = "Full Name",
                primaryPlaceholder = "Enter contact name",
                secondaryLabel = "Email (optional)",
                secondaryPlaceholder = "name@email.com",
                quaternaryLabel = "Job Title (optional)",
                quaternaryPlaceholder = "e.g., Product Manager",
                tertiaryLabel = "Phone (optional)",
                tertiaryPlaceholder = "+1 555 0100"
            )
            QrCategoryType.PHONE -> FormLabels(
                primaryLabel = "Phone Number",
                primaryPlaceholder = "+1 555 0100",
                secondaryLabel = "Display Title / Note",
                secondaryPlaceholder = "Optional label"
            )
            QrCategoryType.EMAIL -> FormLabels(
                primaryLabel = "Email Address",
                primaryPlaceholder = "hello@example.com",
                secondaryLabel = "Subject (optional)",
                secondaryPlaceholder = "Message subject",
                tertiaryLabel = "Body (optional)",
                tertiaryPlaceholder = "Email body"
            )
            QrCategoryType.SMS -> FormLabels(
                primaryLabel = "Phone Number",
                primaryPlaceholder = "+1 555 0100",
                secondaryLabel = "Message (optional)",
                secondaryPlaceholder = "SMS text"
            )
            QrCategoryType.WHATSAPP -> FormLabels(
                primaryLabel = "WhatsApp Number",
                primaryPlaceholder = "15550100 (with country code)",
                secondaryLabel = "Message (optional)",
                secondaryPlaceholder = "Pre-filled chat text"
            )
            QrCategoryType.LOCATION -> FormLabels(
                primaryLabel = "Coordinates (lat,lng)",
                primaryPlaceholder = "37.7749,-122.4194",
                secondaryLabel = "Place label (optional)",
                secondaryPlaceholder = "Golden Gate Bridge"
            )
            QrCategoryType.CALENDAR -> FormLabels(
                primaryLabel = "Event Title",
                primaryPlaceholder = "Team standup",
                secondaryLabel = "Start (YYYYMMDDTHHMMSS, optional)",
                secondaryPlaceholder = "20260921T090000"
            )
            QrCategoryType.APP_LINK -> FormLabels(
                primaryLabel = "App URL or Package",
                primaryPlaceholder = "com.example.app or https://play.google.com/…",
                secondaryLabel = "Display Title / Note",
                secondaryPlaceholder = "Optional label"
            )
            QrCategoryType.FACEBOOK -> socialForm("Facebook", "username or https://facebook.com/…")
            QrCategoryType.YOUTUBE -> socialForm("YouTube", "@channel or https://youtube.com/…")
            QrCategoryType.TWITTER -> socialForm("Twitter", "username or https://x.com/…")
            QrCategoryType.TIKTOK -> socialForm("TikTok", "@username or https://tiktok.com/…")
            QrCategoryType.INSTAGRAM -> socialForm("Instagram", "username or https://instagram.com/…")
            QrCategoryType.PAYPAL -> socialForm("PayPal", "paypal.me name or https://paypal.me/…")
            QrCategoryType.SNAPCHAT -> socialForm("Snapchat", "username or https://snapchat.com/add/…")
            QrCategoryType.LINKEDIN -> socialForm("LinkedIn", "profile name or https://linkedin.com/in/…")
            QrCategoryType.SPOTIFY -> socialForm("Spotify", "user id or https://open.spotify.com/…")
            QrCategoryType.CODE_128 -> barcodeForm("Code 128 value", "Text or numbers")
            QrCategoryType.DATA_MATRIX -> barcodeForm("Data Matrix value", "Text or numbers")
            QrCategoryType.PDF_417 -> barcodeForm("PDF 417 value", "Text or numbers")
            QrCategoryType.AZTEC -> barcodeForm("Aztec value", "Text or numbers")
            QrCategoryType.EAN_13 -> barcodeForm("EAN 13 digits", "12 digits, or 13 with check digit")
            QrCategoryType.EAN_8 -> barcodeForm("EAN 8 digits", "7 digits, or 8 with check digit")
            QrCategoryType.UPC_E -> barcodeForm("UPC E digits", "7 or 8 digits")
            QrCategoryType.UPC_A -> barcodeForm("UPC A digits", "11 digits, or 12 with check digit")
            QrCategoryType.CODE_93 -> barcodeForm("Code 93 value", "ASCII text")
            QrCategoryType.CODE_39 -> barcodeForm("Code 39 value", "Text or numbers")
            QrCategoryType.CODABAR -> barcodeForm("Codabar value", "Digits, optional start/stop")
            QrCategoryType.ITF -> barcodeForm("ITF digits", "Even number of digits")
            else -> FormLabels(
                primaryLabel = "Payload",
                primaryPlaceholder = "Enter value"
            )
        }

        private fun socialForm(name: String, example: String) = FormLabels(
            primaryLabel = "$name username or link",
            primaryPlaceholder = example,
            secondaryLabel = "Display title (optional)",
            secondaryPlaceholder = "Optional label"
        )

        private fun barcodeForm(label: String, example: String) = FormLabels(
            primaryLabel = label,
            primaryPlaceholder = example
        )
    }
}

@Composable
internal fun FormTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = title,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
    }
}

@Composable
internal fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    optional: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Unspecified,
    trailingPaste: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (optional && !label.contains("optional", ignoreCase = true)) {
                    "$label (Optional)"
                } else {
                    label
                },
                color = TextPrimary,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (trailingPaste != null) {
                Row(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = trailingPaste
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentPaste,
                        contentDescription = null,
                        tint = CobaltPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Paste",
                        color = CobaltPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NestedSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(CobaltPrimary),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontFamily = PlusJakartaSans,
                    fontSize = 14.sp
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextTertiary,
                            fontFamily = PlusJakartaSans,
                            fontSize = 14.sp
                        )
                    }
                    inner()
                }
            )
            if (onClear != null && value.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Clear,
                    contentDescription = "Clear",
                    tint = TextTertiary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onClear
                        )
                )
            }
        }
    }
}

@Composable
private fun SecondaryFormAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NestedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = CobaltPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}
