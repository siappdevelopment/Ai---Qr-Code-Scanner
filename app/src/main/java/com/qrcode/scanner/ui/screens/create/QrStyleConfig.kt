package com.qrcode.scanner.ui.screens.create

import java.io.Serializable

/**
 * Stitch Customization Studio style model (White Theme d8bdbae6…).
 * Payload is never stored here — appearance only.
 */
data class QrStyleConfig(
    val bodyPattern: BodyPattern = BodyPattern.CLASSIC,
    val eyeStyle: EyeStyle = EyeStyle.SQUARE,
    val foregroundColor: Int = COLOR_COBALT,
    val backgroundColor: Int = COLOR_WHITE,
    val paletteKey: String = PALETTE_COBALT,
    val paletteName: String = "Electric Cobalt",
    val centerIcon: CenterIcon = CenterIcon.NONE,
    val customLogoUri: String? = null,
    val frame: FrameTemplate = FrameTemplate.NONE,
    val previewDarkCanvas: Boolean = false
) : Serializable {

    enum class BodyPattern(val label: String) : Serializable {
        CLASSIC("Classic (Square)"),
        ROUNDED("Rounded"),
        DOTS("Radial Dots"),
        DIAMOND("Diamond Mesh")
    }

    enum class EyeStyle(val label: String) : Serializable {
        SQUARE("Square"),
        SOFT("Soft Curve"),
        CIRCLE("Circle Eye")
    }

    enum class CenterIcon(val label: String) : Serializable {
        NONE("None"),
        WIFI("Wi-Fi"),
        GLOBE("Globe"),
        CUSTOM("Custom")
    }

    enum class FrameTemplate(val label: String, val subtitle: String) : Serializable {
        NONE("None", "Clean borderless matrix"),
        PILL("Scan Me Pill", "Badge attached footer"),
        BANNER("Banner Top", "Top accent header band"),
        CYBER("Cyber Reticle", "Cobalt HUD view corners")
    }

    companion object {
        private const val serialVersionUID = 1L

        const val COLOR_COBALT = 0xFF0033CC.toInt()
        const val COLOR_SAPPHIRE = 0xFF001A99.toInt()
        /**
         * Neon Cyan module color. Prefer a vivid cyan that still clears the scannability
         * contrast gate on white. `#00B4FF` fails (~2.34); `#007ACC` is ~4.51.
         */
        const val COLOR_CYAN = 0xFF007ACC.toInt()
        const val COLOR_SLATE = 0xFF0F172A.toInt()
        const val COLOR_WHITE = 0xFFFFFFFF.toInt()

        const val PALETTE_COBALT = "cobalt"
        const val PALETTE_SAPPHIRE = "sapphire"
        const val PALETTE_CYAN = "cyan"
        const val PALETTE_SLATE = "slate"
        const val PALETTE_CUSTOM = "custom"

        val Default = QrStyleConfig()
    }
}
