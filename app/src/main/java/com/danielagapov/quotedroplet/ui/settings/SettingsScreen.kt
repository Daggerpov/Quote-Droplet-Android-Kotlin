package com.danielagapov.quotedroplet.ui.settings

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TextFields
import com.danielagapov.quotedroplet.ui.components.ActionButton
import com.danielagapov.quotedroplet.ui.components.IconTile
import com.danielagapov.quotedroplet.ui.components.QuoteSerif
import com.danielagapov.quotedroplet.ui.components.SectionLabel
import com.danielagapov.quotedroplet.ui.theme.PALETTE_DISPLAY_ORDER
import com.danielagapov.quotedroplet.ui.theme.PALETTE_NAMES
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
                (QuoteCategory.Saved to container.localQuotes.bookmarks.value.size) +
                (QuoteCategory.Custom to container.localQuotes.customs.value.size)
        }
    }
}

@Composable
fun SettingsScreen(onOpenInfo: () -> Unit) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        HeaderBar("Settings", onOpenInfo, subtitle = "Widget & app")
        PaletteSegmentedControl(
            labels = listOf("Appearance", "Quotes"),
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel("Widget preview")
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
            label = "Widget font",
            options = WidgetFonts.all.indices.toList(),
            selected = fontIndex,
            onSelect = {
                settings.setFontIndex(it)
                WidgetSync.onAppearanceChanged(context)
            },
            optionText = { WidgetFonts[it].name },
            optionFont = { WidgetFonts[it].composeFamily() },
            icon = Icons.Filled.TextFields,
        )

        SectionLabel("Color palette")
        PaletteCard(padding = 14.dp) {
            // Dark presets on the first row, light ones on the second; the custom palette has its own card
            PALETTE_DISPLAY_ORDER.chunked(4).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { index ->
                        PaletteSwatch(
                            swatch = presetPalettes[index],
                            name = PALETTE_NAMES[index],
                            selected = paletteIndex == index,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                settings.setPaletteIndex(index)
                                WidgetSync.onAppearanceChanged(context)
                            },
                        )
                    }
                }
            }
        }

        SectionLabel("Custom palette")
        val customPalette = Palette(colorFromHex(customHex[0]), colorFromHex(customHex[1]), colorFromHex(customHex[2]))
        PaletteCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PaletteSwatch(
                    swatch = customPalette,
                    name = "Custom",
                    selected = paletteIndex == CUSTOM_PALETTE_INDEX,
                    modifier = Modifier.width(76.dp),
                    onClick = {
                        settings.setPaletteIndex(CUSTOM_PALETTE_INDEX)
                        WidgetSync.onAppearanceChanged(context)
                    },
                )
                Spacer(Modifier.width(12.dp))
                customHex.forEachIndexed { slot, hex ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorFromHex(hex))
                                .border(2.dp, palette.hairline, CircleShape)
                                .clickable { editingSlot = slot },
                        )
                        Text(
                            listOf("Background", "Text", "Accent")[slot],
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
        Text(
            "Tap a color to change it. Your colors theme the app and the widget.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
    }

    editingSlot?.let { slot ->
        ColorPickerDialog(
            initial = colorFromHex(customHex[slot]),
            title = "Custom ${listOf("background", "text", "accent").getOrElse(slot) { "" }} color",
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
private fun WidgetPreview(palette: Palette, fontIndex: Int) {
    val family = WidgetFonts[fontIndex].composeFamily()
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(158.dp)
            .shadow(if (palette.isDark) 12.dp else 6.dp, shape)
            .clip(shape)
            .background(palette.background)
            // Outline keeps the preview visible when it matches the screen color
            .border(1.dp, palette.hairline, shape)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "More is lost by indecision than by wrong decision.",
                fontFamily = family,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text("— Cicero", fontFamily = family, fontSize = 15.sp, color = palette.accent, maxLines = 1)
        }
    }
}

/** A palette tile: its background with "Aa" in its text color and an accent bar, plus its name. */
@Composable
private fun PaletteSwatch(
    swatch: Palette,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(shape)
                .background(swatch.background)
                .border(if (selected) 3.dp else 1.dp, if (selected) palette.accent else palette.hairline, shape)
                .padding(10.dp),
        ) {
            Column {
                Text("Aa", fontFamily = QuoteSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = swatch.text)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(24.dp).height(4.dp).clip(CircleShape).background(swatch.accent))
            }
            if (selected) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(palette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = palette.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) palette.text else palette.secondaryText,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionLabel("Widget")
        PickerRow(
            icon = Icons.Filled.GridView,
            label = "Quote category",
            options = QuoteCategory.entries,
            selected = category,
            onSelect = {
                settings.setQuoteCategory(it)
                WidgetSync.onCategoryChanged(context)
            },
            optionText = { c -> counts[c]?.let { "${c.displayName} ($it)" } ?: c.displayName },
        )
        PickerRow(
            icon = Icons.Filled.Autorenew,
            label = "New quote",
            options = QuoteFrequency.entries,
            selected = frequency,
            onSelect = {
                settings.setQuoteFrequency(it)
                WidgetSync.onFrequencyChanged(context)
            },
            optionText = { it.displayName },
        )
        Text(
            "The category also picks which quotes your notifications show.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        )
        // Same gap above this section as between the other settings sections
        Spacer(Modifier.height(10.dp))
        SectionLabel("Notifications")
        PaletteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Filled.NotificationsActive)
                Spacer(Modifier.width(14.dp))
                Text(
                    "Daily quote notification",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                        checkedThumbColor = palette.onAccent,
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.secondaryText,
                    modifier = Modifier.padding(top = 10.dp),
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

        ActionButton(
            text = "Change notification time",
            icon = Icons.Outlined.Alarm,
            onClick = { showTimePicker = true },
            modifier = Modifier.fillMaxWidth(),
            primary = false,
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
