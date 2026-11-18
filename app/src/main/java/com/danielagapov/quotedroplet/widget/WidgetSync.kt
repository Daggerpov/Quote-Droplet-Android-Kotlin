package com.danielagapov.quotedroplet.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.danielagapov.quotedroplet.appContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteCategory
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Keeps the home screen widget's quote fresh and redrawn when settings change. */
object WidgetSync {
    val FALLBACK_QUOTE = Quote(
        id = 0,
        text = "More is lost by indecision than by wrong decision.",
        author = "Cicero",
        classification = "wisdom",
        likes = 0,
    )
    private val NO_SAVED_QUOTES = Quote(
        id = -2,
        text = "No saved quotes yet. Save quotes in the app to see them here.",
        author = "Quote Droplet",
    )

    private const val PERIODIC_WORK = "quote-widget-periodic"
    private const val ONE_TIME_WORK = "quote-widget-now"
    private const val MAX_LENGTH = 110

    suspend fun hasWidgets(context: Context): Boolean =
        GlanceAppWidgetManager(context).getGlanceIds(QuoteWidget::class.java).isNotEmpty()

    suspend fun ensureScheduled(context: Context) {
        if (hasWidgets(context)) schedulePeriodic(context, replace = false) else cancel(context)
    }

    fun schedulePeriodic(context: Context, replace: Boolean) {
        val hours = context.appContainer.settings.quoteFrequency.value.hours
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(hours, TimeUnit.HOURS)
            .setInitialDelay(hours, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
    }

    suspend fun redraw(context: Context) {
        if (hasWidgets(context)) QuoteWidget().updateAll(context)
    }

    /** Colors or font changed: redraw right away. */
    fun onAppearanceChanged(context: Context) {
        context.appContainer.appScope.launch { redraw(context) }
    }

    /** Category changed: show a quote from the new category right away. */
    fun onCategoryChanged(context: Context) {
        context.appContainer.appScope.launch { if (hasWidgets(context)) refreshNow(context) }
    }

    fun onFrequencyChanged(context: Context) {
        context.appContainer.appScope.launch { if (hasWidgets(context)) schedulePeriodic(context, replace = true) }
    }

    /** Puts a new quote on the widget (and in Droplets → Recent). */
    suspend fun loadNewQuote(context: Context): Quote {
        val container = context.appContainer
        val category = container.settings.quoteCategory.value
        val quote = if (category == QuoteCategory.Saved) {
            container.localQuotes.bookmarks.value.randomOrNull() ?: NO_SAVED_QUOTES
        } else {
            withTimeoutOrNull(12_000) { container.repository.randomQuote(category, MAX_LENGTH).getOrNull() }
                ?: container.bundledQuotes.random(category, MAX_LENGTH)
                ?: FALLBACK_QUOTE
        }
        container.widgetQuote.set(quote)
        if (quote.id > 0) container.localQuotes.addRecent(quote)
        return quote
    }

    /** Asks the launcher to add the widget (Android 8+, on launchers that support it). */
    fun requestPin(context: Context): Boolean {
        val manager = context.getSystemService(AppWidgetManager::class.java) ?: return false
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, QuoteWidgetReceiver::class.java), null, null)
    }
}

class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        WidgetSync.loadNewQuote(applicationContext)
        QuoteWidget().updateAll(applicationContext)
        return Result.success()
    }
}
