package com.danielagapov.quotedroplet.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

/**
 * One of the app's color palettes. Like the iOS app, the selected widget palette themes the whole
 * app: [background] fills the screens, [text] is for quotes, [accent] for authors, selection and
 * actions. The other colors are derived so every palette gets the same card/hairline treatment.
 */
@Immutable
data class Palette(val background: Color, val text: Color, val accent: Color) {
    val colors: List<Color> get() = listOf(background, text, accent)
    val isDark: Boolean get() = background.luminance() < 0.3f

    /** Cards lift off the background: lighter on dark palettes, near-white on light ones. */
    val card: Color get() = if (isDark) lerp(background, text, 0.08f) else lerp(background, Color.White, 0.62f)

    /** A second, stronger surface (secondary buttons, the navigation bar). */
    val raised: Color get() = if (isDark) lerp(background, text, 0.14f) else lerp(background, text, 0.06f)

    val secondaryText: Color get() = text.copy(alpha = 0.68f)

    val hairline: Color get() = text.copy(alpha = if (isDark) 0.12f else 0.10f)

    /** Text on top of the accent color (filled buttons, selected segments). */
    val onAccent: Color get() = if (accent.luminance() > 0.4f) Color(0xFF15151A) else Color.White

    /** Bottom of the subtle screen gradient. */
    val backgroundBottom: Color get() = lerp(background, Color.Black, if (isDark) 0.22f else 0.06f)
}

const val CUSTOM_PALETTE_INDEX = 3

// Starting point for the custom palette (forest, cream, blush); users' saved colors are kept.
val DEFAULT_CUSTOM_HEX = listOf("#17573D", "#F4EFE6", "#F0BCCD")

// Same palettes, in the same order, as the iOS app (index 3 holds the custom colors). Every preset
// keeps quote text above 7:1 contrast and the accent above 4.5:1 against the background.
private val presetHex = listOf(
    listOf("3A2C24", "F7EEDD", "E8B66B"), // Espresso
    listOf("DCEEF8", "0F2536", "1D6696"), // Sky
    listOf("F6F0E6", "3A2E27", "A9492A"), // Linen
    listOf("17573D", "F4EFE6", "F0BCCD"), // Custom (default colors)
    listOf("0C1A2E", "F1E4CE", "DDA978"), // Midnight
    listOf("1E1D2A", "F4F2F8", "F59A62"), // Ember
    listOf("E6ECE2", "1E2B22", "3F6E4E"), // Sage
    listOf("2D383E", "EEF4F2", "EBAFC3"), // Rosewood
    listOf("18191E", "E9EAEE", "A4AEC2"), // Graphite
)

val PALETTE_NAMES = listOf("Espresso", "Sky", "Linen", "Custom", "Midnight", "Ember", "Sage", "Rosewood", "Graphite")

/** The order presets are shown in Settings: dark ones, then light ones. */
val PALETTE_DISPLAY_ORDER = listOf(0, 4, 5, 7, 8, 1, 2, 6)

val presetPalettes: List<Palette> = presetHex.map { (bg, text, accent) ->
    Palette(colorFromHex(bg), colorFromHex(text), colorFromHex(accent))
}

val PALETTE_COUNT: Int = presetPalettes.size

fun paletteFor(index: Int, custom: List<Color>): Palette =
    if (index == CUSTOM_PALETTE_INDEX && custom.size >= 3) Palette(custom[0], custom[1], custom[2])
    else presetPalettes.getOrElse(index) { presetPalettes[0] }

fun colorFromHex(hex: String): Color {
    val value = hex.trim().removePrefix("#").take(6).toLongOrNull(16) ?: 0L
    return Color(0xFF000000L or value)
}

fun Color.toHex(): String = String.format(Locale.US, "#%06X", toArgb() and 0xFFFFFF)

val LocalPalette = staticCompositionLocalOf { presetPalettes[0] }
