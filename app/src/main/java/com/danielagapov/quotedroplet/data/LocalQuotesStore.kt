package com.danielagapov.quotedroplet.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Liked, saved (bookmarked) and custom quotes, kept on the device like the iOS app's UserDefaults. */
class LocalQuotesStore(context: Context, private val json: Json) {
    private val prefs = context.getSharedPreferences("local_quotes", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(Quote.serializer())

    private val _liked = MutableStateFlow(read(KEY_LIKED))
    private val _likedIds = MutableStateFlow(_liked.value.ids())
    val likedIds: StateFlow<Set<Int>> = _likedIds.asStateFlow()

    private val _bookmarks = MutableStateFlow(read(KEY_BOOKMARKS))
    /** Saved quotes, oldest first. */
    val bookmarks: StateFlow<List<Quote>> = _bookmarks.asStateFlow()
    private val _bookmarkIds = MutableStateFlow(_bookmarks.value.ids())
    val bookmarkIds: StateFlow<Set<Int>> = _bookmarkIds.asStateFlow()

    private val _customs = MutableStateFlow(read(KEY_CUSTOM))
    /** The user's own quotes from Droplets → Custom, oldest first. */
    val customs: StateFlow<List<Quote>> = _customs.asStateFlow()

    /** Called after any change, so the home screen widget can redraw its heart. */
    var onChanged: (() -> Unit)? = null

    fun isLiked(id: Int): Boolean = id in _likedIds.value
    fun isBookmarked(id: Int): Boolean = id in _bookmarkIds.value

    @Synchronized
    fun setLiked(quote: Quote, liked: Boolean) {
        // Remove first so re-liking never creates duplicates
        val list = _liked.value.filterNot { it.id == quote.id }.let { if (liked) it + quote else it }
        write(KEY_LIKED, list)
        _liked.value = list
        _likedIds.value = list.ids()
        onChanged?.invoke()
    }

    @Synchronized
    fun setBookmarked(quote: Quote, bookmarked: Boolean) {
        val list = _bookmarks.value.filterNot { it.id == quote.id }.let { if (bookmarked) it + quote else it }
        write(KEY_BOOKMARKS, list)
        _bookmarks.value = list
        _bookmarkIds.value = list.ids()
        onChanged?.invoke()
    }

    /** Adds a custom quote, or replaces the one with the same ID (an edit). */
    @Synchronized
    fun saveCustom(quote: Quote) {
        val current = _customs.value
        val list = if (current.any { it.id == quote.id }) current.map { if (it.id == quote.id) quote else it } else current + quote
        write(KEY_CUSTOM, list)
        _customs.value = list
    }

    @Synchronized
    fun deleteCustom(quote: Quote) {
        val list = _customs.value.filterNot { it.id == quote.id }
        write(KEY_CUSTOM, list)
        _customs.value = list
    }

    private fun List<Quote>.ids(): Set<Int> = mapTo(HashSet()) { it.id }

    private fun read(key: String): List<Quote> = prefs.getString(key, null)
        ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
        ?: emptyList()

    private fun write(key: String, list: List<Quote>) =
        prefs.edit { putString(key, json.encodeToString(serializer, list)) }

    private companion object {
        const val KEY_LIKED = "liked_quotes"
        const val KEY_BOOKMARKS = "bookmarked_quotes"
        const val KEY_CUSTOM = "custom_quotes"
    }
}
