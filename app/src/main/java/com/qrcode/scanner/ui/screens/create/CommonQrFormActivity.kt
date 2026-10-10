package com.qrcode.scanner.ui.screens.create

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.qrcode.scanner.ui.theme.enableThemedEdgeToEdge
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.res.stringResource
import com.qrcode.scanner.app.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrcode.scanner.data.settings.SettingsPreferences
import com.qrcode.scanner.data.settings.SettingsRepositoryProvider
import com.qrcode.scanner.ui.theme.BorderSubtle
import com.qrcode.scanner.ui.components.AppBackButton
import com.qrcode.scanner.ui.components.appHeaderBackground
import com.qrcode.scanner.ui.theme.CardSurface
import com.qrcode.scanner.ui.theme.CobaltPrimary
import com.qrcode.scanner.ui.theme.CobaltSoft
import com.qrcode.scanner.ui.theme.Destructive
import com.qrcode.scanner.ui.theme.NestedSurface
import com.qrcode.scanner.ui.theme.PageBackground
import com.qrcode.scanner.ui.theme.PlusJakartaSans
import com.qrcode.scanner.ui.theme.QRCodeScannerTheme
import com.qrcode.scanner.ui.components.ScreenWithAd
import com.qrcode.scanner.ui.components.navigationBarsPaddingUnlessKeyboard
import com.qrcode.scanner.ui.components.runWithClickAd
import com.qrcode.scanner.ui.components.bindScreenBackAd
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
        enableThemedEdgeToEdge()
        // The ad stays at the screen bottom; ScreenWithAd(keyboardAware) shrinks only the form above the keyboard.
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        bindScreenBackAd("QrFormScreen")
        val type = QrCategoryType.fromIntentExtra(
            intent.getStringExtra(QrCategoryType.EXTRA_QR_CATEGORY)
        )
        val nativeSize = if (FormLabels.forCategory(type, this).editRowCount() <= 2) {
            "medium"
        } else {
            "small"
        }
        setContent {
            QRCodeScannerTheme {
                ScreenWithAd(screenKey = "QrFormScreen", nativeSize = nativeSize, keyboardAware = true) {
                CommonQrFormScreen(
                    category = type,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onSavedClose = { finish() }
                )
                }
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
    val labels = remember(category, context) { FormLabels.forCategory(category, context) }
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

    fun currentInput() = QrPayloadBuilder.FormInput(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        quaternary = quaternary
    )

    fun capField(raw: String, field: Int, barcode: Boolean = false): String {
        val filtered = if (barcode) BarcodeSymbology.filterInput(category, raw) else raw
        val max = QrPayloadBuilder.fieldMaxLength(category, field)
        return if (filtered.length > max) {
            error = QrPayloadBuilder.limitMessage(max, context)
            filtered.take(max)
        } else {
            if (error != null) error = null
            filtered
        }
    }

    fun attemptGenerate() {
        val input = currentInput()
        val validation = QrPayloadBuilder.validate(category, input, context)
        if (validation != null) {
            error = validation
            return
        }
        error = null
        val built = QrPayloadBuilder.build(category, input)
        context.runWithClickAd("QrFormScreen") {
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
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .navigationBarsPaddingUnlessKeyboard()
    ) {
        FormTopBar(title = category.displayTitle(context), onBack = onBack)

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
                        text = stringResource(R.string.create_barcode_stub_title),
                        color = TextPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.create_barcode_stub_body),
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
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(CobaltSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = category.displayTitle(context),
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
                        primary = capField(it, 0, BarcodeSymbology.isBarcode(category))
                    },
                    placeholder = labels.primaryPlaceholder,
                    keyboardType = labels.primaryKeyboard,
                    trailingPaste = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrBlank()) {
                            primary = capField(text, 0, BarcodeSymbology.isBarcode(category))
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_clipboard_empty),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onClear = { primary = "" }
                )
                if (labels.secondaryLabel != null && category == QrCategoryType.CALENDAR) {
                    FormDateField(
                        label = labels.secondaryLabel,
                        value = secondary,
                        onValueChange = {
                            secondary = it
                            if (error != null) error = null
                        },
                        placeholder = labels.secondaryPlaceholder.orEmpty()
                    )
                } else if (labels.secondaryLabel != null) {
                    FormTextField(
                        label = labels.secondaryLabel,
                        value = secondary,
                        onValueChange = { secondary = capField(it, 1) },
                        placeholder = labels.secondaryPlaceholder.orEmpty(),
                        optional = labels.secondaryOptional
                    )
                }
                if (labels.quaternaryLabel != null) {
                    FormTextField(
                        label = labels.quaternaryLabel,
                        value = quaternary,
                        onValueChange = { quaternary = capField(it, 3) },
                        placeholder = labels.quaternaryPlaceholder.orEmpty(),
                        optional = labels.quaternaryOptional
                    )
                }
                if (labels.tertiaryLabel != null) {
                    FormTextField(
                        label = labels.tertiaryLabel,
                        value = tertiary,
                        onValueChange = { tertiary = capField(it, 2) },
                        placeholder = labels.tertiaryPlaceholder.orEmpty(),
                        optional = labels.tertiaryOptional,
                        keyboardType = labels.tertiaryKeyboard
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
                        text = stringResource(
                            if (BarcodeSymbology.isBarcode(category)) {
                                R.string.create_form_generate_preview
                            } else {
                                R.string.create_form_generate_preview_qr
                            }
                        ),
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
                    label = stringResource(R.string.create_form_instant_share),
                    icon = Icons.Outlined.Share,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val input = currentInput()
                        val validation = QrPayloadBuilder.validate(category, input, context)
                        if (validation != null) {
                            error = validation
                            return@SecondaryFormAction
                        }
                        val built = QrPayloadBuilder.build(category, input)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, built.payload)
                        }
                        context.startActivity(
                            Intent.createChooser(
                                send,
                                context.getString(R.string.share_chooser_title)
                            )
                        )
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
    val tertiaryOptional: Boolean = true,
    val quaternaryLabel: String? = null,
    val quaternaryPlaceholder: String? = null,
    val quaternaryOptional: Boolean = true,
    val primaryKeyboard: KeyboardType = KeyboardType.Text,
    val tertiaryKeyboard: KeyboardType = KeyboardType.Text
) {
    fun editRowCount(): Int {
        var count = 1
        if (secondaryLabel != null) count++
        if (tertiaryLabel != null) count++
        if (quaternaryLabel != null) count++
        return count
    }

    companion object {
        fun forCategory(type: QrCategoryType, context: Context): FormLabels {
            val noteLabel = context.getString(R.string.form_display_title_note_label)
            val notePlaceholder = context.getString(R.string.form_display_title_note_placeholder)
            val optionalLabel = context.getString(R.string.form_optional_label_placeholder)
            val socialNote = context.getString(R.string.form_social_display_title_label)
            return when (type) {
                QrCategoryType.WEBSITE -> FormLabels(
                    primaryLabel = context.getString(R.string.form_website_url_label),
                    primaryPlaceholder = context.getString(R.string.form_website_url_placeholder),
                    secondaryLabel = noteLabel,
                    secondaryPlaceholder = notePlaceholder
                )
                QrCategoryType.PLAIN_TEXT -> FormLabels(
                    primaryLabel = context.getString(R.string.form_plain_text_label),
                    primaryPlaceholder = context.getString(R.string.form_plain_text_placeholder),
                    secondaryLabel = noteLabel,
                    secondaryPlaceholder = optionalLabel
                )
                QrCategoryType.CONTACT -> FormLabels(
                    primaryLabel = context.getString(R.string.form_contact_name_label),
                    primaryPlaceholder = context.getString(R.string.form_contact_name_placeholder),
                    secondaryLabel = context.getString(R.string.form_contact_email_label),
                    secondaryPlaceholder = context.getString(R.string.form_contact_email_placeholder),
                    quaternaryLabel = context.getString(R.string.form_contact_job_title_label),
                    quaternaryPlaceholder = context.getString(R.string.form_contact_job_title_placeholder),
                    tertiaryLabel = context.getString(R.string.form_contact_phone_label),
                    tertiaryPlaceholder = context.getString(R.string.form_contact_phone_placeholder),
                    tertiaryKeyboard = KeyboardType.Phone
                )
                QrCategoryType.PHONE -> FormLabels(
                    primaryLabel = context.getString(R.string.form_phone_number_label),
                    primaryPlaceholder = context.getString(R.string.form_contact_phone_placeholder),
                    secondaryLabel = noteLabel,
                    secondaryPlaceholder = optionalLabel,
                    primaryKeyboard = KeyboardType.Phone
                )
                QrCategoryType.EMAIL -> FormLabels(
                    primaryLabel = context.getString(R.string.form_email_address_label),
                    primaryPlaceholder = context.getString(R.string.form_email_address_placeholder),
                    secondaryLabel = context.getString(R.string.form_email_subject_label),
                    secondaryPlaceholder = context.getString(R.string.form_email_subject_placeholder),
                    tertiaryLabel = context.getString(R.string.form_email_body_label),
                    tertiaryPlaceholder = context.getString(R.string.form_email_body_placeholder),
                    tertiaryOptional = false
                )
                QrCategoryType.SMS -> FormLabels(
                    primaryLabel = context.getString(R.string.form_phone_number_label),
                    primaryPlaceholder = context.getString(R.string.form_contact_phone_placeholder),
                    secondaryLabel = context.getString(R.string.form_sms_message_label),
                    secondaryPlaceholder = context.getString(R.string.form_sms_message_placeholder),
                    secondaryOptional = false,
                    primaryKeyboard = KeyboardType.Phone
                )
                QrCategoryType.WHATSAPP -> FormLabels(
                    primaryLabel = context.getString(R.string.form_whatsapp_number_label),
                    primaryPlaceholder = context.getString(R.string.form_whatsapp_number_placeholder),
                    secondaryLabel = context.getString(R.string.form_whatsapp_message_label),
                    secondaryPlaceholder = context.getString(R.string.form_whatsapp_message_placeholder),
                    primaryKeyboard = KeyboardType.Phone
                )
                QrCategoryType.LOCATION -> FormLabels(
                    primaryLabel = context.getString(R.string.form_location_coords_label),
                    primaryPlaceholder = context.getString(R.string.form_location_coords_placeholder),
                    secondaryLabel = context.getString(R.string.form_location_place_label),
                    secondaryPlaceholder = context.getString(R.string.form_location_place_placeholder)
                )
                QrCategoryType.CALENDAR -> FormLabels(
                    primaryLabel = "Event Title",
                    primaryPlaceholder = "Team standup",
                    secondaryLabel = "Start Date",
                    secondaryPlaceholder = "Select date",
                    secondaryOptional = false
                )
                QrCategoryType.APP_LINK -> FormLabels(
                    primaryLabel = context.getString(R.string.form_app_link_label),
                    primaryPlaceholder = context.getString(R.string.form_app_link_placeholder),
                    secondaryLabel = noteLabel,
                    secondaryPlaceholder = optionalLabel
                )
                QrCategoryType.FACEBOOK -> socialForm(
                    context,
                    R.string.form_social_facebook_primary_label,
                    R.string.form_social_facebook_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.YOUTUBE -> socialForm(
                    context,
                    R.string.form_social_youtube_primary_label,
                    R.string.form_social_youtube_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.TWITTER -> socialForm(
                    context,
                    R.string.form_social_twitter_primary_label,
                    R.string.form_social_twitter_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.TIKTOK -> socialForm(
                    context,
                    R.string.form_social_tiktok_primary_label,
                    R.string.form_social_tiktok_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.INSTAGRAM -> socialForm(
                    context,
                    R.string.form_social_instagram_primary_label,
                    R.string.form_social_instagram_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.PAYPAL -> socialForm(
                    context,
                    R.string.form_social_paypal_primary_label,
                    R.string.form_social_paypal_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.SNAPCHAT -> socialForm(
                    context,
                    R.string.form_social_snapchat_primary_label,
                    R.string.form_social_snapchat_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.LINKEDIN -> socialForm(
                    context,
                    R.string.form_social_linkedin_primary_label,
                    R.string.form_social_linkedin_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.SPOTIFY -> socialForm(
                    context,
                    R.string.form_social_spotify_primary_label,
                    R.string.form_social_spotify_primary_placeholder,
                    socialNote,
                    optionalLabel
                )
                QrCategoryType.CODE_128 -> barcodeForm(
                    context,
                    R.string.form_barcode_code_128_label,
                    R.string.form_barcode_code_128_placeholder
                )
                QrCategoryType.DATA_MATRIX -> barcodeForm(
                    context,
                    R.string.form_barcode_data_matrix_label,
                    R.string.form_barcode_generic_placeholder
                )
                QrCategoryType.PDF_417 -> barcodeForm(
                    context,
                    R.string.form_barcode_pdf_417_label,
                    R.string.form_barcode_generic_placeholder
                )
                QrCategoryType.AZTEC -> barcodeForm(
                    context,
                    R.string.form_barcode_aztec_label,
                    R.string.form_barcode_generic_placeholder
                )
                QrCategoryType.EAN_13 -> barcodeForm(
                    context,
                    R.string.form_barcode_ean_13_label,
                    R.string.form_barcode_ean_13_placeholder,
                    numeric = true
                )
                QrCategoryType.EAN_8 -> barcodeForm(
                    context,
                    R.string.form_barcode_ean_8_label,
                    R.string.form_barcode_ean_8_placeholder,
                    numeric = true
                )
                QrCategoryType.UPC_E -> barcodeForm(
                    context,
                    R.string.form_barcode_upc_e_label,
                    R.string.form_barcode_upc_e_placeholder,
                    numeric = true
                )
                QrCategoryType.UPC_A -> barcodeForm(
                    context,
                    R.string.form_barcode_upc_a_label,
                    R.string.form_barcode_upc_a_placeholder,
                    numeric = true
                )
                QrCategoryType.CODE_93 -> barcodeForm(
                    context,
                    R.string.form_barcode_code_93_label,
                    R.string.form_barcode_code_93_placeholder
                )
                QrCategoryType.CODE_39 -> barcodeForm(
                    context,
                    R.string.form_barcode_code_39_label,
                    R.string.form_barcode_generic_placeholder
                )
                QrCategoryType.CODABAR -> barcodeForm(
                    context,
                    R.string.form_barcode_codabar_label,
                    R.string.form_barcode_codabar_placeholder
                )
                QrCategoryType.ITF -> barcodeForm(
                    context,
                    R.string.form_barcode_itf_label,
                    R.string.form_barcode_itf_placeholder,
                    numeric = true
                )
                else -> FormLabels(
                    primaryLabel = context.getString(R.string.form_payload_label),
                    primaryPlaceholder = context.getString(R.string.form_payload_placeholder)
                )
            }
        }

        private fun socialForm(
            context: Context,
            primaryLabelRes: Int,
            primaryPlaceholderRes: Int,
            secondaryLabel: String,
            secondaryPlaceholder: String
        ) = FormLabels(
            primaryLabel = context.getString(primaryLabelRes),
            primaryPlaceholder = context.getString(primaryPlaceholderRes),
            secondaryLabel = secondaryLabel,
            secondaryPlaceholder = secondaryPlaceholder
        )

        private fun barcodeForm(
            context: Context,
            labelRes: Int,
            placeholderRes: Int,
            numeric: Boolean = false
        ) = FormLabels(
            primaryLabel = context.getString(labelRes),
            primaryPlaceholder = context.getString(placeholderRes),
            primaryKeyboard = if (numeric) KeyboardType.Number else KeyboardType.Text
        )
    }
}

@Composable
internal fun FormTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .appHeaderBackground()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppBackButton(onClick = onBack)
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
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingPaste: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null
) {
    val optionalSuffix = stringResource(R.string.form_label_optional_suffix, label)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (optional && !label.contains("optional", ignoreCase = true)) {
                    optionalSuffix
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
                        text = stringResource(R.string.action_paste),
                        color = CobaltPrimary,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
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
                    contentDescription = stringResource(R.string.cd_clear),
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
internal fun FormDateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val context = LocalContext.current
    val display = formatPickedDate(value)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = TextPrimary,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NestedSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { showCalendarDatePicker(context, value, onValueChange) }
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = display.ifBlank { placeholder },
                color = if (display.isBlank()) TextTertiary else TextPrimary,
                fontFamily = PlusJakartaSans,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = "Pick date",
                tint = CobaltPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun showCalendarDatePicker(
    context: android.content.Context,
    current: String,
    onValueChange: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    val stamp = QrPayloadBuilder.normalizeCalendarStamp(current)
    if (stamp != null) {
        calendar.set(Calendar.YEAR, stamp.substring(0, 4).toInt())
        calendar.set(Calendar.MONTH, stamp.substring(4, 6).toInt() - 1)
        calendar.set(Calendar.DAY_OF_MONTH, stamp.substring(6, 8).toInt())
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            onValueChange(String.format(Locale.US, "%04d%02d%02d", year, month + 1, day))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun formatPickedDate(raw: String): String {
    val stamp = QrPayloadBuilder.normalizeCalendarStamp(raw) ?: return ""
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, stamp.substring(0, 4).toInt())
        set(Calendar.MONTH, stamp.substring(4, 6).toInt() - 1)
        set(Calendar.DAY_OF_MONTH, stamp.substring(6, 8).toInt())
    }
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
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
