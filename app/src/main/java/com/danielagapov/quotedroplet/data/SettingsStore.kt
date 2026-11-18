package com.danielagapov.quotedroplet.data

import android.content.Context
import androidx.core.content.edit
import com.danielagapov.quotedroplet.ui.theme.CUSTOM_PALETTE_INDEX
import com.danielagapov.quotedroplet.ui.theme.DEFAULT_CUSTOM_HEX
import com.danielagapov.quotedroplet.ui.theme.PALETTE_COUNT
import com.danielagapov.quotedroplet.ui.theme.Palette
import com.danielagapov.quotedroplet.ui.theme.colorFromHex
import com.danielagapov.quotedroplet.ui.theme.paletteFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

data class NotificationSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    /** False while the time is still the one picked automatically on first launch. */
    val customized: Boolean,
)

/** App + widget settings (the iOS app's "group.selectedSettings" UserDefaults). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _paletteIndex = MutableStateFlow(prefs.getInt(KEY_PALETTE, 0).coerceIn(0, PALETTE_COUNT - 1))
    val paletteIndex: StateFlow<Int> = _paletteIndex.asStateFlow()

    private val _customColors = MutableStateFlow(
        List(3) { i -> prefs.getString(customKey(i), null) ?: DEFAULT_CUSTOM_HEX[i] }
    )
    /** The three custom palette colors as hex strings. */
    val customColors: StateFlow<List<String>> = _customColors.asStateFlow()

    private val _palette = MutableStateFlow(computePalette())
    val palette: StateFlow<Palette> = _palette.asStateFlow()

    private val _fontIndex = MutableStateFlow(prefs.getInt(KEY_FONT, 0).coerceIn(0, WidgetFonts.all.lastIndex))
    val fontIndex: StateFlow<Int> = _fontIndex.asStateFlow()

    private val _quoteCategory = MutableStateFlow(QuoteCategory.fromKey(prefs.getString(KEY_CATEGORY, null)))
    /** Category used by the widget and daily notifications. */
    val quoteCategory: StateFlow<QuoteCategory> = _quoteCategory.asStateFlow()

    private val _quoteFrequency = MutableStateFlow(QuoteFrequency.fromKey(prefs.getString(KEY_FREQUENCY, null)))
    /** How often the widget switches quotes. */
    val quoteFrequency: StateFlow<QuoteFrequency> = _quoteFrequency.asStateFlow()

    private val _notifications = MutableStateFlow(readNotifications())
    val notifications: StateFlow<NotificationSettings> = _notifications.asStateFlow()

    var submitterName: String
        get() = prefs.getString(KEY_SUBMITTER_NAME, "") ?: ""
        set(value) = prefs.edit { putString(KEY_SUBMITTER_NAME, value) }

    fun setPaletteIndex(index: Int) {
        val clamped = index.coerceIn(0, PALETTE_COUNT - 1)
        prefs.edit { putInt(KEY_PALETTE, clamped) }
        _paletteIndex.value = clamped
        _palette.value = computePalette()
    }

    /** Changing a custom color also switches to the custom palette, like the iOS app. */
    fun setCustomColor(slot: Int, hex: String) {
        prefs.edit { putString(customKey(slot), hex) }
        _customColors.value = _customColors.value.toMutableList().also { it[slot] = hex }
        setPaletteIndex(CUSTOM_PALETTE_INDEX)
    }

    fun setFontIndex(index: Int) {
        prefs.edit { putInt(KEY_FONT, index) }
        _fontIndex.value = index
    }

    fun setQuoteCategory(category: QuoteCategory) {
        prefs.edit { putString(KEY_CATEGORY, category.name) }
        _quoteCategory.value = category
    }

    fun setQuoteFrequency(frequency: QuoteFrequency) {
        prefs.edit { putString(KEY_FREQUENCY, frequency.name) }
        _quoteFrequency.value = frequency
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt(KEY_NOTIF_HOUR, hour)
            putInt(KEY_NOTIF_MINUTE, minute)
            putBoolean(KEY_NOTIF_CUSTOMIZED, true)
            putBoolean(KEY_NOTIF_ENABLED, true)
        }
        _notifications.value = readNotifications()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIF_ENABLED, enabled) }
        _notifications.value = readNotifications()
    }

    /** First launch: like iOS, the first daily quote arrives 3 minutes after opening the app. */
    fun ensureNotificationTimeChosen() {
        if (prefs.contains(KEY_NOTIF_HOUR)) return
        val time = LocalTime.now().plusMinutes(3)
        prefs.edit {
            putInt(KEY_NOTIF_HOUR, time.hour)
            putInt(KEY_NOTIF_MINUTE, time.minute)
            putBoolean(KEY_NOTIF_CUSTOMIZED, false)
            putBoolean(KEY_NOTIF_ENABLED, true)
        }
        _notifications.value = readNotifications()
    }

    /** Counts likes, saves and copies; returns the new total. */
    fun incrementInteractions(): Int {
        val next = prefs.getInt(KEY_INTERACTIONS, 0) + 1
        prefs.edit { putInt(KEY_INTERACTIONS, next) }
        return next
    }

    private fun readNotifications() = NotificationSettings(
        enabled = prefs.getBoolean(KEY_NOTIF_ENABLED, true),
        hour = prefs.getInt(KEY_NOTIF_HOUR, 9),
        minute = prefs.getInt(KEY_NOTIF_MINUTE, 0),
        customized = prefs.getBoolean(KEY_NOTIF_CUSTOMIZED, false),
    )

    private fun computePalette(): Palette =
        paletteFor(_paletteIndex.value, _customColors.value.map(::colorFromHex))

    private companion object {
        const val KEY_PALETTE = "widget_color_palette_index"
        const val KEY_FONT = "selected_font_index"
        const val KEY_CATEGORY = "quote_category"
        const val KEY_FREQUENCY = "quote_frequency"
        const val KEY_NOTIF_ENABLED = "notifications_enabled"
        const val KEY_NOTIF_HOUR = "notification_hour"
        const val KEY_NOTIF_MINUTE = "notification_minute"
        const val KEY_NOTIF_CUSTOMIZED = "notification_time_customized"
        const val KEY_SUBMITTER_NAME = "submitter_name"
        const val KEY_INTERACTIONS = "interactions"

        fun customKey(slot: Int) = "custom_color_$slot"
    }
}
