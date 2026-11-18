package com.danielagapov.quotedroplet.ui.settings

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danielagapov.quotedroplet.ui.components.PaletteTextField
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import com.danielagapov.quotedroplet.ui.theme.colorFromHex
import com.danielagapov.quotedroplet.ui.theme.toHex

/** "Daily Notification Scheduling" sheet from the iOS app, as a dialog with a clock picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTimeDialog(
    hour: Int,
    minute: Int,
    customized: Boolean,
    onDismiss: () -> Unit,
    onDone: (Int, Int) -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val state = rememberTimePickerState(hour, minute, DateFormat.is24HourFormat(context))
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = palette.background,
            contentColor = palette.text,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Daily Notification Scheduling",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.text,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "You currently have daily notifications ${if (customized) "" else "automatically "}scheduled for:\n${formatTime(context, hour, minute)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = palette.card,
                        clockDialSelectedContentColor = palette.background,
                        clockDialUnselectedContentColor = palette.text,
                        selectorColor = palette.accent,
                        containerColor = palette.background,
                        periodSelectorBorderColor = palette.accent,
                        periodSelectorSelectedContainerColor = palette.accent.copy(alpha = 0.3f),
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = palette.text,
                        periodSelectorUnselectedContentColor = palette.text,
                        timeSelectorSelectedContainerColor = palette.accent.copy(alpha = 0.3f),
                        timeSelectorUnselectedContainerColor = palette.card,
                        timeSelectorSelectedContentColor = palette.text,
                        timeSelectorUnselectedContentColor = palette.text,
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = palette.text) }
                    TextButton(onClick = { onDone(state.hour, state.minute) }) {
                        Text("Done", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** A simple hue / saturation / brightness picker with a hex field, for the custom palette. */
@Composable
fun ColorPickerDialog(initial: Color, title: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val palette = LocalPalette.current
    val start = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    var hexText by remember { mutableStateOf(initial.toHex()) }

    fun setFromColor(c: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(c.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = palette.background, contentColor = palette.text) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = palette.text)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color)
                        .border(1.dp, palette.text.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                )
                LabeledSlider(
                    label = "Hue",
                    value = hue / 360f,
                    onChange = {
                        hue = it * 360f
                        hexText = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))).toHex()
                    },
                    track = Brush.horizontalGradient(
                        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map {
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(it, 1f, 1f)))
                        }
                    ),
                )
                LabeledSlider(
                    label = "Saturation",
                    value = saturation,
                    onChange = {
                        saturation = it
                        hexText = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))).toHex()
                    },
                    track = Brush.horizontalGradient(
                        listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))),
                        )
                    ),
                )
                LabeledSlider(
                    label = "Brightness",
                    value = value,
                    onChange = {
                        value = it
                        hexText = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))).toHex()
                    },
                    track = Brush.horizontalGradient(
                        listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f))))
                    ),
                )
                PaletteTextField(
                    value = hexText,
                    onValueChange = { text ->
                        hexText = text.uppercase().take(7)
                        val clean = text.trim().removePrefix("#")
                        if (clean.length == 6 && clean.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                            setFromColor(colorFromHex(clean))
                        }
                    },
                    label = "Hex",
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = palette.text) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { onPick(color.toHex()) }) {
                        Text("Use Color", color = palette.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit, track: Brush) {
    val palette = LocalPalette.current
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = palette.text)
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(track)
            )
            Slider(
                value = value,
                onValueChange = onChange,
                colors = SliderDefaults.colors(
                    thumbColor = palette.text,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().height(36.dp),
            )
        }
    }
}
