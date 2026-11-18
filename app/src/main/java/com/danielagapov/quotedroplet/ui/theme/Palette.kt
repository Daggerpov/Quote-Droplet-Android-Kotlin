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
 * app: [background] fills the screens, [text] is for quotes, [accent] for authors, titles and icons.
 */
@Immutable
data class Palette(val background: Color, val text: Color, val accent: Color) {
    val colors: List<Color> get() = listOf(background, text, accent)
    val isDark: Boolean get() = background.luminance() < 0.4f

    /** Cards sit a little apart from the background. */
    val card: Color get() = lerp(background, text, 0.07f)

    /** Bottom of the subtle screen gradient. */
    val backgroundBottom: Color get() = lerp(background, Color.Black, if (isDark) 0.18f else 0.08f)
}

const val CUSTOM_PALETTE_INDEX = 3
val DEFAULT_CUSTOM_HEX = listOf("#1C7C54", "#E2B6CF", "#DEF4C6")

// Same palettes, in the same order, as the iOS app (index 3 holds the custom colors)
private val presetHex = listOf(
    listOf("504136", "EEC584", "F8F3E1"),
    listOf("85C7F2", "0C1618", "83781B"),
    listOf("EFF8E2", "DC9E82", "423E37"),
    listOf("1C7C54", "E2B6CF", "DEF4C6"),
    listOf("001427", "EDD4B2", "D0A98F"),
    listOf("242434", "F79256", "F0EFF4"),
    listOf("A0B9C6", "33658A", "2F4858"),
    listOf("39474F", "E7F3F1", "DEAABE"),
    listOf("2A2E40", "DADFE3", "909AAB"),
)

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
