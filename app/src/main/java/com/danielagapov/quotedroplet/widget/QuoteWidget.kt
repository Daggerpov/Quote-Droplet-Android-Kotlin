package com.danielagapov.quotedroplet.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.danielagapov.quotedroplet.MainActivity
import com.danielagapov.quotedroplet.R
import com.danielagapov.quotedroplet.appContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.WidgetFont
import com.danielagapov.quotedroplet.data.WidgetFonts
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.ui.theme.Palette

/** The home screen widget: a quote in the chosen palette and font, with like and next buttons. */
class QuoteWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = context.appContainer
        if (container.widgetQuote.quote.value == null) WidgetSync.refreshNow(context)
        provideContent {
            val quote by container.widgetQuote.quote.collectAsState()
            val palette by container.settings.palette.collectAsState()
            val fontIndex by container.settings.fontIndex.collectAsState()
            val likedIds by container.localQuotes.likedIds.collectAsState()
            val likeCounts by container.repository.likeCounts.collectAsState()
            val shown = quote ?: WidgetSync.FALLBACK_QUOTE
            WidgetBody(
                quote = shown,
                palette = palette,
                font = WidgetFonts[fontIndex],
                liked = shown.id in likedIds,
                likes = likeCounts[shown.id] ?: shown.likes ?: 0,
            )
        }
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 250.dp)
    }
}

@Composable
private fun WidgetBody(quote: Quote, palette: Palette, font: WidgetFont, liked: Boolean, likes: Int) {
    val size = LocalSize.current
    val small = size.width < 200.dp
    val large = size.height >= 220.dp
    val quoteSize = when {
        large -> 22.sp
        small -> 14.sp
        else -> 16.sp
    }
    val authorSize = when {
        large -> 17.sp
        small -> 12.sp
        else -> 14.sp
    }
    val family = FontFamily(font.family)
    val accent = ColorProvider(palette.accent)
    // Quotes with a real id can be liked; placeholders (no saved quotes, offline fallback) can't
    val interactive = !small && quote.id > 0

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(20.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = quote.text,
            style = TextStyle(
                color = ColorProvider(palette.text),
                fontSize = quoteSize,
                fontFamily = family,
                textAlign = TextAlign.Center,
            ),
            maxLines = if (large) 10 else if (small) 6 else 4,
        )
        Spacer(GlanceModifier.height(6.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isAuthorValid(quote.author)) {
                Text(
                    text = "— ${quote.author.orEmpty().trim()}",
                    style = TextStyle(color = accent, fontSize = authorSize, fontFamily = family),
                    maxLines = 1,
                )
            }
            if (interactive) {
                Spacer(GlanceModifier.width(10.dp))
                Image(
                    provider = ImageProvider(if (liked) R.drawable.ic_widget_heart else R.drawable.ic_widget_heart_outline),
                    contentDescription = if (liked) "Unlike" else "Like",
                    colorFilter = ColorFilter.tint(accent),
                    modifier = GlanceModifier.size(22.dp).clickable(actionRunCallback<LikeWidgetQuoteAction>()),
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(text = "$likes", style = TextStyle(color = accent, fontSize = authorSize))
                Spacer(GlanceModifier.width(12.dp))
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_next),
                    contentDescription = "Next quote",
                    colorFilter = ColorFilter.tint(accent),
                    modifier = GlanceModifier.size(22.dp).clickable(actionRunCallback<NextWidgetQuoteAction>()),
                )
            }
        }
    }
}

class LikeWidgetQuoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val container = context.appContainer
        val quote = container.widgetQuote.quote.value ?: return
        if (quote.id <= 0) return
        container.repository.toggleLikeNow(quote)
        QuoteWidget().updateAll(context)
    }
}

class NextWidgetQuoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WidgetSync.loadNewQuote(context)
        QuoteWidget().updateAll(context)
    }
}

class QuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuoteWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetSync.refreshNow(context)
        WidgetSync.schedulePeriodic(context, replace = false)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetSync.cancel(context)
    }
}
