package com.danielagapov.quotedroplet

import android.content.Context
import com.danielagapov.quotedroplet.data.BundledQuotes
import com.danielagapov.quotedroplet.data.LocalQuotesStore
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteApi
import com.danielagapov.quotedroplet.data.QuoteRepository
import com.danielagapov.quotedroplet.data.SettingsStore
import com.danielagapov.quotedroplet.data.SubmitterId
import com.danielagapov.quotedroplet.data.WidgetQuoteStore
import com.danielagapov.quotedroplet.widget.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** App-wide singletons (simple manual dependency injection). */
class AppContainer(private val context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .header("User-Agent", "QuoteDroplet-Android/${BuildConfig.VERSION_NAME} okhttp")
                    .build()
            )
        }
        .build()

    private val api: QuoteApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(QuoteApi::class.java)

    val settings = SettingsStore(context)
    val localQuotes = LocalQuotesStore(context, json)
    val widgetQuote = WidgetQuoteStore(context, json)
    val bundledQuotes = BundledQuotes(context, json)
    val repository = QuoteRepository(api, localQuotes, appScope)
    val submitterId: String by lazy { SubmitterId.getOrCreate(context) }

    /** Emits once the user has had enough interactions to be asked for a Play review. */
    val reviewRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Set when the app is opened from a notification, so Droplets shows its Recent page. */
    val openRecentRequests = MutableStateFlow(false)

    init {
        // Keep the widget's heart in sync with likes made in the app
        localQuotes.onChanged = { appScope.launch { WidgetSync.redraw(context) } }
    }

    fun toggleLike(quote: Quote) {
        repository.toggleLike(quote)
        recordInteraction()
    }

    fun toggleBookmark(quote: Quote) {
        localQuotes.setBookmarked(quote, !localQuotes.isBookmarked(quote.id))
        recordInteraction()
    }

    fun recordInteraction() {
        // Same moment as the iOS app's review prompt
        if (settings.incrementInteractions() == REVIEW_PROMPT_AT) reviewRequests.tryEmit(Unit)
    }

    private companion object {
        const val REVIEW_PROMPT_AT = 21
    }
}
