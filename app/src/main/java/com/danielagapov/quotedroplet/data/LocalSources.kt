package com.danielagapov.quotedroplet.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID

/** The quotes database snapshot bundled with the app, for notifications and the widget when offline. */
class BundledQuotes(private val context: Context, private val json: Json) {
    private val quotes: List<Quote> by lazy {
        runCatching {
            context.assets.open("quotes_backup.json").bufferedReader().use {
                json.decodeFromString(ListSerializer(Quote.serializer()), it.readText())
            }
        }.getOrDefault(emptyList())
    }

    fun random(category: QuoteCategory, maxLength: Int): Quote? {
        val short = quotes.filter { it.text.length <= maxLength }
        val pool = if (category == QuoteCategory.All) short
        else short.filter { it.classification.equals(category.apiName, ignoreCase = true) }
        return pool.randomOrNull()
    }
}

/** The quote currently on the home screen widget. */
class WidgetQuoteStore(context: Context, private val json: Json) {
    private val prefs = context.getSharedPreferences("widget", Context.MODE_PRIVATE)
    private val _quote = MutableStateFlow(
        prefs.getString(KEY, null)?.let { runCatching { json.decodeFromString(Quote.serializer(), it) }.getOrNull() }
    )
    val quote: StateFlow<Quote?> = _quote.asStateFlow()

    fun set(quote: Quote) {
        prefs.edit { putString(KEY, json.encodeToString(Quote.serializer(), quote)) }
        _quote.value = quote
    }

    private companion object {
        const val KEY = "widget_quote"
    }
}

/**
 * Anonymous per-install ID sent with quote submissions, so the user can later see their own
 * submissions. Android's auto backup carries it over to a reinstall or a new phone.
 */
object SubmitterId {
    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences("submitter", Context.MODE_PRIVATE)
        prefs.getString(KEY, null)?.let { return it }
        val id = UUID.randomUUID().toString().uppercase()
        prefs.edit { putString(KEY, id) }
        return id
    }

    private const val KEY = "submitter_id"
}

/** Fonts for the widget: Android's built-in font families (the iOS ones aren't on Android). */
data class WidgetFont(val name: String, val family: String)

object WidgetFonts {
    val all = listOf(
        WidgetFont("Serif", "serif"),
        WidgetFont("Sans Serif", "sans-serif"),
        WidgetFont("Light", "sans-serif-light"),
        WidgetFont("Condensed", "sans-serif-condensed"),
        WidgetFont("Monospace", "monospace"),
        WidgetFont("Serif Mono", "serif-monospace"),
        WidgetFont("Casual", "casual"),
        WidgetFont("Cursive", "cursive"),
        WidgetFont("Small Caps", "sans-serif-smallcaps"),
    )

    operator fun get(index: Int): WidgetFont = all.getOrElse(index) { all[0] }
}
