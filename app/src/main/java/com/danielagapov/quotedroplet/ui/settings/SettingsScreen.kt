package com.danielagapov.quotedroplet.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.data.QuoteFrequency
import com.danielagapov.quotedroplet.data.WidgetFonts
import com.danielagapov.quotedroplet.notifications.QuoteNotifications
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.PaletteCard
import com.danielagapov.quotedroplet.ui.components.PaletteSegmentedControl
import com.danielagapov.quotedroplet.ui.components.PickerRow
import com.danielagapov.quotedroplet.ui.theme.CUSTOM_PALETTE_INDEX
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import com.danielagapov.quotedroplet.ui.theme.Palette
import com.danielagapov.quotedroplet.ui.theme.colorFromHex
import com.danielagapov.quotedroplet.ui.theme.composeFamily
import com.danielagapov.quotedroplet.ui.theme.presetPalettes
import com.danielagapov.quotedroplet.widget.WidgetSync
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val _counts = MutableStateFlow<Map<QuoteCategory, Int>>(emptyMap())
    val counts: StateFlow<Map<QuoteCategory, Int>> = _counts.asStateFlow()

    fun loadCounts() {
        viewModelScope.launch {
            val remote = QuoteCategory.remote.map { category ->
                async { category to container.repository.count(category) }
            }.awaitAll()
            _counts.value = remote.mapNotNull { (c, n) -> n?.let { c to it } }.toMap() +
                (QuoteCategory.Saved to container.localQuotes.bookmarks.value.size)
        }
    }
}

@Composable
fun SettingsScreen(onOpenInfo: () -> Unit) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        HeaderBar(onOpenInfo)
        PaletteSegmentedControl(
            labels = listOf("Appearance", "Quotes"),
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) AppearancePage() else QuotesPage()
        }
    }
}

@Composable
private fun AppearancePage() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val palette = LocalPalette.current
    val settings = container.settings
    val paletteIndex by settings.paletteIndex.collectAsStateWithLifecycle()
    val customHex by settings.customColors.collectAsStateWithLifecycle()
    val fontIndex by settings.fontIndex.collectAsStateWithLifecycle()
    var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var pinUnsupported by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WidgetPreview(palette, fontIndex)
        TextButton(onClick = { pinUnsupported = !WidgetSync.requestPin(context) }) {
            Icon(Icons.Outlined.Widgets, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.size(6.dp))
            Text("Add the widget to your home screen", color = palette.accent, fontWeight = FontWeight.SemiBold)
        }
        if (pinUnsupported) {
            Text(
                "Your launcher can't add widgets from apps. Long-press your home screen, tap Widgets, then pick Quote Droplet.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
        }
        PickerRow(
            label = "Widget Font:",
            options = WidgetFonts.all.indices.toList(),
            selected = fontIndex,
            onSelect = {
                settings.setFontIndex(it)
                WidgetSync.onAppearanceChanged(context)
            },
            optionText = { WidgetFonts[it].name },
            optionFont = { WidgetFonts[it].composeFamily() },
        )

        SectionLabel("Sample Colors:")
        // Same layout as iOS: four presets per row, the custom palette has its own row
        val rows = listOf(listOf(0, 1, 2, 4), listOf(5, 6, 7, 8))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { index ->
                    PaletteSwatch(
                        colors = presetPalettes[index].colors,
                        selected = paletteIndex == index,
                        onClick = {
                            settings.setPaletteIndex(index)
                            WidgetSync.onAppearanceChanged(context)
                        },
                    )
                }
            }
        }

        SectionLabel("Custom Colors:")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            customHex.forEachIndexed { slot, hex ->
                PaletteSwatch(
                    colors = listOf(colorFromHex(hex)),
                    selected = paletteIndex == CUSTOM_PALETTE_INDEX,
                    onClick = { editingSlot = slot },
                    label = listOf("Background", "Quote", "Author")[slot],
                )
            }
        }
        Text(
            "Tap a custom color to change it. Your colors theme the app and the widget.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.text.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }

    editingSlot?.let { slot ->
        ColorPickerDialog(
            initial = colorFromHex(customHex[slot]),
            title = "Custom ${listOf("background", "quote", "author").getOrElse(slot) { "" }} color",
            onDismiss = { editingSlot = null },
            onPick = { hex ->
                settings.setCustomColor(slot, hex)
                WidgetSync.onAppearanceChanged(context)
                editingSlot = null
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = LocalPalette.current.text,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun WidgetPreview(palette: Palette, fontIndex: Int) {
    val family = WidgetFonts[fontIndex].composeFamily()
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .size(170.dp)
            .shadow(10.dp, shape)
            .clip(shape)
            .background(palette.background)
            // Outline keeps the preview visible when it matches the screen color (dark palettes)
            .border(1.dp, palette.text.copy(alpha = 0.18f), shape)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "More is lost by indecision than by wrong decision.",
                fontFamily = family,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text("— Cicero", fontFamily = family, fontSize = 14.sp, color = palette.accent, maxLines = 1)
        }
    }
}

@Composable
private fun PaletteSwatch(
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    label: String? = null,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(12.dp)
    val brush = if (colors.size > 1) Brush.linearGradient(colors) else Brush.linearGradient(listOf(colors[0], colors[0]))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(64.dp)
                .clip(shape)
                .background(brush)
                .border(if (selected) 3.dp else 1.dp, if (selected) palette.accent else palette.text.copy(alpha = 0.25f), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected && label == null) {
                val tint = if (colors[0].luminance() < 0.5f) Color.White else Color.Black
                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = tint)
            }
        }
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.text, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun QuotesPage() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val palette = LocalPalette.current
    val settings = container.settings
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val counts by vm.counts.collectAsStateWithLifecycle()
    val category by settings.quoteCategory.collectAsStateWithLifecycle()
    val frequency by settings.quoteFrequency.collectAsStateWithLifecycle()
    val notifications by settings.notifications.collectAsStateWithLifecycle()
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var permissionCheck by remember { mutableIntStateOf(0) }
    val notificationsAllowed = remember(permissionCheck) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionCheck++
    }
    LaunchedEffect(Unit) { vm.loadCounts() }
    // Re-check permission when coming back from system settings
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { permissionCheck++ }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PickerRow(
            label = "Quote Category:",
            options = QuoteCategory.entries,
            selected = category,
            onSelect = {
                settings.setQuoteCategory(it)
                WidgetSync.onCategoryChanged(context)
            },
            optionText = { c -> counts[c]?.let { "${c.displayName} ($it)" } ?: c.displayName },
        )
        PickerRow(
            label = "Reload Widget:",
            options = QuoteFrequency.entries,
            selected = frequency,
            onSelect = {
                settings.setQuoteFrequency(it)
                WidgetSync.onFrequencyChanged(context)
            },
            optionText = { it.displayName },
        )
        Text(
            "The category picks quotes for your widget and daily notifications.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.text.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )

        PaletteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Daily quote notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = notifications.enabled,
                    onCheckedChange = {
                        settings.setNotificationsEnabled(it)
                        QuoteNotifications.schedule(context, settings)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.background,
                        checkedTrackColor = palette.accent,
                        uncheckedThumbColor = palette.text.copy(alpha = 0.7f),
                        uncheckedTrackColor = palette.card,
                        uncheckedBorderColor = palette.accent.copy(alpha = 0.6f),
                    ),
                )
            }
            if (notifications.enabled) {
                Text(
                    "You currently have daily notifications ${if (notifications.customized) "" else "automatically "}scheduled for ${formatTime(context, notifications.hour, notifications.minute)}.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.accent,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (!notificationsAllowed) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Notifications are turned off for Quote Droplet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.text,
                        modifier = Modifier.weight(1f),
                    )
                }
                TextButton(onClick = {
                    val canAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (canAsk) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) {
                    Text("Turn on notifications", color = palette.accent, fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedActionButton(
            text = "Schedule Notifications",
            icon = Icons.Outlined.Alarm,
            onClick = { showTimePicker = true },
        )
    }

    if (showTimePicker) {
        NotificationTimeDialog(
            hour = notifications.hour,
            minute = notifications.minute,
            customized = notifications.customized,
            onDismiss = { showTimePicker = false },
            onDone = { hour, minute ->
                settings.setNotificationTime(hour, minute)
                QuoteNotifications.schedule(context, settings)
                showTimePicker = false
            },
        )
    }
}

fun formatTime(context: android.content.Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern))
}
