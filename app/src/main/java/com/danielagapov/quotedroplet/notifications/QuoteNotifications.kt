package com.danielagapov.quotedroplet.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.danielagapov.quotedroplet.MainActivity
import com.danielagapov.quotedroplet.R
import com.danielagapov.quotedroplet.appContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.data.SettingsStore
import com.danielagapov.quotedroplet.data.quoteWithAuthor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime

/** The daily quote notification (the iOS app's NotificationSchedulerService). */
object QuoteNotifications {
    const val CHANNEL_ID = "daily_quotes"
    private const val ACTION_DAILY_QUOTE = "com.danielagapov.quotedroplet.DAILY_QUOTE"
    private const val ALARM_REQUEST_CODE = 4201
    private const val NOTIFICATION_ID = 4202

    // Inexact alarms need no special permission; Android delivers them within this window
    private const val WINDOW_MILLIS = 10 * 60 * 1000L
    private const val NOTIFICATION_MAX_LENGTH = 100

    fun createChannel(context: Context) {
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.water_drip}")
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)arms the alarm for the next daily quote, or cancels it when notifications are off. */
    fun schedule(context: Context, settings: SettingsStore) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        alarmManager.cancel(pending)
        val s = settings.notifications.value
        if (!s.enabled) return
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            nextTriggerMillis(s.hour, s.minute, ZonedDateTime.now()),
            WINDOW_MILLIS,
            pending,
        )
    }

    fun nextTriggerMillis(hour: Int, minute: Int, now: ZonedDateTime): Long {
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, QuoteAlarmReceiver::class.java).setAction(ACTION_DAILY_QUOTE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Picks a short quote for the chosen category and posts it. */
    @SuppressLint("MissingPermission") // checked with areNotificationsEnabled()
    suspend fun postDailyQuote(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val (title, quote) = pickQuote(context)

        val body = quoteWithAuthor(quote)
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_RECENT, quote.id > 0)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_droplet)
            .setColor(0xFF1E88E5.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            return
        }
        // Shown quotes appear in Droplets → Recent
        if (quote.id > 0) context.appContainer.localQuotes.addRecent(quote)
    }

    private suspend fun pickQuote(context: Context): Pair<String, Quote> {
        val container = context.appContainer
        val category = container.settings.quoteCategory.value
        if (category == QuoteCategory.Saved) {
            val saved = container.localQuotes.bookmarks.value.randomOrNull()
            return if (saved != null) "Quote Droplet: Saved Quotes" to saved
            else "Quote Droplet: No Saved Quotes Added" to Quote(
                id = -1,
                text = "Please add a quote to saved by tapping the save button under a quote in the app's \"Droplets\" tab",
            )
        }
        val title = if (category == QuoteCategory.All) "Quote Droplet" else "Quote Droplet: ${category.displayName}"
        val fromApi = withTimeoutOrNull(6_000) {
            container.repository.randomQuote(category, NOTIFICATION_MAX_LENGTH).getOrNull()
        }
        val quote = fromApi
            ?: container.bundledQuotes.random(category, NOTIFICATION_MAX_LENGTH)
            ?: container.bundledQuotes.random(QuoteCategory.All, NOTIFICATION_MAX_LENGTH)
            ?: Quote(id = -1, text = "More is lost by indecision than by wrong decision.", author = "Cicero")
        return title to quote
    }
}

/** Fires once a day: posts the quote, then arms tomorrow's alarm. */
class QuoteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val container = context.appContainer
        container.appScope.launch {
            try {
                QuoteNotifications.postDailyQuote(context)
            } finally {
                QuoteNotifications.schedule(context, container.settings)
                result.finish()
            }
        }
    }
}

/** Alarms don't survive reboots or clock changes, so re-arm them. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        QuoteNotifications.schedule(context, context.appContainer.settings)
    }
}
