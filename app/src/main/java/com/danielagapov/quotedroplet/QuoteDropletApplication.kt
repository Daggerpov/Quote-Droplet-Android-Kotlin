package com.danielagapov.quotedroplet

import android.app.Application
import android.content.Context
import com.danielagapov.quotedroplet.notifications.QuoteNotifications
import com.danielagapov.quotedroplet.widget.WidgetSync
import kotlinx.coroutines.launch

class QuoteDropletApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        QuoteNotifications.createChannel(this)
        container.settings.ensureNotificationTimeChosen()
        QuoteNotifications.schedule(this, container.settings)
        container.appScope.launch { WidgetSync.ensureScheduled(this@QuoteDropletApplication) }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as QuoteDropletApplication).container
