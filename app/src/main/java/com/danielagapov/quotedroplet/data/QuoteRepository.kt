package com.danielagapov.quotedroplet.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

sealed interface SubmitOutcome {
    val message: String

    data object Success : SubmitOutcome {
        override val message =
            "Thanks for submitting a quote. It is now awaiting approval to be added to this app's quote database."
    }

    data object Duplicate : SubmitOutcome {
        override val message =
            "Thanks for submitting a quote.\n\nIt happens to already exist in the database, though. Great minds think alike."
    }

    data object Failed : SubmitOutcome {
        override val message =
            "Your quote couldn't be sent. Please check your connection and try again."
    }
}

class QuoteRepository(
    private val api: QuoteApi,
    private val local: LocalQuotesStore,
    private val scope: CoroutineScope,
) {
    // Latest like count we know of per quote, so every screen (and the widget) shows the same number
    private val _likeCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val likeCounts: StateFlow<Map<Int, Int>> = _likeCounts.asStateFlow()

    private val likesInFlight: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    fun likesFor(quote: Quote): Int = _likeCounts.value[quote.id] ?: quote.likes ?: 0

    suspend fun randomQuote(category: QuoteCategory, maxLength: Int? = null): Result<Quote> =
        call { api.randomQuote(category.apiFilter, maxLength) }.onSuccess(::remember)

    suspend fun quote(id: Int): Result<Quote> = call { api.quote(id) }.onSuccess(::remember)

    suspend fun quotesByAuthor(author: String): Result<List<Quote>> =
        call { api.quotes(author = author) }.onSuccess(::rememberAll)

    suspend fun search(text: String, category: QuoteCategory): Result<List<Quote>> =
        call { api.quotes(search = text, category = category.apiFilter) }.onSuccess(::rememberAll)

    suspend fun recentQuotes(limit: Int): Result<List<Quote>> =
        call { api.recentQuotes(limit) }.onSuccess(::rememberAll)

    suspend fun topQuotes(category: QuoteCategory): Result<List<Quote>> =
        call { api.topQuotes(category.apiFilter ?: "all") }.onSuccess(::rememberAll)

    suspend fun count(category: QuoteCategory): Int? =
        call { api.count(category.apiFilter ?: "all").count }.getOrNull()

    suspend fun submissions(submitterId: String): Result<List<Submission>> =
        call { api.submissions(submitterId) }

    suspend fun submitQuote(
        text: String,
        author: String,
        category: QuoteCategory,
        submitterName: String,
        submitterId: String,
    ): SubmitOutcome {
        val request = NewQuoteRequest(
            text = text.trim(),
            author = author.trim().ifEmpty { null },
            classification = category.apiName,
            submitterName = submitterName.trim().ifEmpty { null },
            submitterId = submitterId,
        )
        val response = call { api.addQuote(request) }.getOrNull() ?: return SubmitOutcome.Failed
        return when {
            response.isSuccessful -> SubmitOutcome.Success
            response.code() == 409 -> SubmitOutcome.Duplicate
            else -> SubmitOutcome.Failed
        }
    }

    suspend fun sendFeedback(type: FeedbackType, message: String, email: String, appVersion: String): Boolean {
        val prefix = if (type == FeedbackType.Content) "[Content] " else ""
        val content = "$prefix${message.trim()}\n\n(Sent from Quote Droplet for Android $appVersion)"
        val response = call { api.sendFeedback(type.apiType, email.trim(), content) }.getOrNull()
        return response?.isSuccessful == true
    }

    /** Fetches fresh copies of these quotes in the background so their like counts are current. */
    fun refreshLikeCounts(ids: Collection<Int>) {
        if (ids.isEmpty()) return
        scope.launch {
            ids.distinct().take(60).map { id -> async { call { api.quote(id) }.onSuccess(::remember) } }.awaitAll()
        }
    }

    /** Flips the like right away and syncs it with the backend (ignores taps while one is in flight). */
    fun toggleLike(quote: Quote) {
        if (!likesInFlight.add(quote.id)) return
        scope.launch {
            try {
                toggleLikeNow(quote)
            } finally {
                likesInFlight.remove(quote.id)
            }
        }
    }

    suspend fun toggleLikeNow(quote: Quote) {
        // Read the stored state first, so the API call always matches what the user sees
        val wasLiked = local.isLiked(quote.id)
        val before = likesFor(quote)
        local.setLiked(quote, !wasLiked)
        setCount(quote.id, max(0, before + if (wasLiked) -1 else 1))
        call { if (wasLiked) api.unlike(quote.id) else api.like(quote.id) }.onSuccess(::remember)
    }

    private fun remember(quote: Quote) {
        quote.likes?.let { setCount(quote.id, it) }
    }

    private fun rememberAll(quotes: List<Quote>) {
        val known = quotes.mapNotNull { q -> q.likes?.let { q.id to it } }
        if (known.isNotEmpty()) _likeCounts.update { it + known }
    }

    private fun setCount(id: Int, likes: Int) = _likeCounts.update { it + (id to likes) }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
