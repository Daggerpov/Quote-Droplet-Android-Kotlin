package com.danielagapov.quotedroplet.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import com.danielagapov.quotedroplet.data.WidgetFont

@Composable
fun QuoteDropletTheme(palette: Palette, content: @Composable () -> Unit) {
    val card = palette.card
    val raised = lerp(palette.background, palette.text, 0.12f)
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.text,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.background,
            onSurface = palette.text,
            surfaceVariant = card,
            onSurfaceVariant = palette.text,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
            outline = palette.accent,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.text,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.background,
            onSurface = palette.text,
            surfaceVariant = card,
            onSurfaceVariant = palette.text,
            surfaceContainerLowest = palette.background,
            surfaceContainerLow = card,
            surfaceContainer = card,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = raised,
            outline = palette.accent,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** The widget font as a Compose font family (for the widget preview in Settings). */
fun WidgetFont.composeFamily(): FontFamily = FontFamily(Typeface.create(family, Typeface.NORMAL))
