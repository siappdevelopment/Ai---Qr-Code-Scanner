package com.qrcode.scanner.ui.screens.create

import androidx.annotation.StringRes
import com.qrcode.scanner.app.R
import java.io.Serializable

/**
 * Stitch Customization Studio style model (White Theme d8bdbae6…).
 * Payload is never stored here — appearance only.
 */
data class QrStyleConfig(
    val bodyPattern: BodyPattern = BodyPattern.CLASSIC,
    val eyeStyle: EyeStyle = EyeStyle.SQUARE,
    val foregroundColor: Int = COLOR_BLACK,
    val backgroundColor: Int = COLOR_WHITE,
    val paletteKey: String = PALETTE_BLACK,
    val paletteName: String = "Black",
    val centerIcon: CenterIcon = CenterIcon.NONE,
    val customLogoUri: String? = null,
    val frame: FrameTemplate = FrameTemplate.NONE,
    val previewDarkCanvas: Boolean = false
) : Serializable {

    enum class BodyPattern(@StringRes val labelRes: Int) : Serializable {
        CLASSIC(R.string.qr_pattern_classic),
        ROUNDED(R.string.qr_pattern_rounded),
        DOTS(R.string.qr_pattern_dots),
        DIAMOND(R.string.qr_pattern_diamond)
    }

    enum class EyeStyle(@StringRes val labelRes: Int) : Serializable {
        SQUARE(R.string.qr_eye_square),
        SOFT(R.string.qr_eye_soft),
        CIRCLE(R.string.qr_eye_circle)
    }

    enum class CenterIcon(@StringRes val labelRes: Int) : Serializable {
        NONE(R.string.qr_icon_none),
        WIFI(R.string.qr_icon_wifi),
        GLOBE(R.string.qr_icon_globe),
        CUSTOM(R.string.qr_icon_custom)
    }

    enum class FrameTemplate(
        @StringRes val labelRes: Int,
        @StringRes val subtitleRes: Int
    ) : Serializable {
        NONE(R.string.qr_frame_none_title, R.string.qr_frame_none_subtitle),
        PILL(R.string.qr_frame_pill_title, R.string.qr_frame_pill_subtitle),
        BANNER(R.string.qr_frame_banner_title, R.string.qr_frame_banner_subtitle),
        CYBER(R.string.qr_frame_cyber_title, R.string.qr_frame_cyber_subtitle)
    }

    companion object {
        private const val serialVersionUID = 1L

        const val COLOR_BLACK = 0xFF000000.toInt()
        const val COLOR_COBALT = 0xFF0033CC.toInt()
        const val COLOR_SAPPHIRE = 0xFF001A99.toInt()
        /**
         * Neon Cyan module color. Prefer a vivid cyan that still clears the scannability
         * contrast gate on white. `#00B4FF` fails (~2.34); `#007ACC` is ~4.51.
         */
        const val COLOR_CYAN = 0xFF007ACC.toInt()
        const val COLOR_SLATE = 0xFF0F172A.toInt()
        const val COLOR_WHITE = 0xFFFFFFFF.toInt()

        const val PALETTE_BLACK = "black"
        const val PALETTE_COBALT = "cobalt"
        const val PALETTE_SAPPHIRE = "sapphire"
        const val PALETTE_CYAN = "cyan"
        const val PALETTE_SLATE = "slate"
        const val PALETTE_CUSTOM = "custom"

        val Default = QrStyleConfig()
    }
}
