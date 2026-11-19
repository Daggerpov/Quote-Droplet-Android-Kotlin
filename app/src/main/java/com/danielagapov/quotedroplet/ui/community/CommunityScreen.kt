package com.danielagapov.quotedroplet.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.danielagapov.quotedroplet.ui.components.QuoteSerif
import com.danielagapov.quotedroplet.ui.components.SectionLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.actionButtonWidth
import com.danielagapov.quotedroplet.ui.components.PageTitle
import com.danielagapov.quotedroplet.ui.components.PaletteCard
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class CommunityViewModel(private val container: AppContainer) : ViewModel() {
    private val _newest = MutableStateFlow<List<Quote>>(emptyList())
    val newest: StateFlow<List<Quote>> = _newest.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun load() {
        viewModelScope.launch {
            container.repository.recentQuotes(limit = 3)
                .onSuccess {
                    _newest.value = it
                    _failed.value = false
                }
                .onFailure { _failed.value = _newest.value.isEmpty() }
        }
    }
}

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatDate(instant: Instant): String = dateFormatter.format(instant.atZone(ZoneId.systemDefault()))

@Composable
fun CommunityScreen(onOpenInfo: () -> Unit, onOpenSubmissions: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: CommunityViewModel = viewModel { CommunityViewModel(container) }
    val newest by vm.newest.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        HeaderBar("Community", onOpenInfo, subtitle = "From readers")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SubmitCallout(onOpenSubmissions)
            NewestQuotes(newest, failed)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SubmitCallout(onOpenSubmissions: () -> Unit) {
    val palette = LocalPalette.current
    PaletteCard(padding = 20.dp) {
        Text("Know a quote that belongs here?", fontFamily = QuoteSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.text)
        Text(
            "Submit it, and once it's approved it shows up for everyone, credited to you if you like.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SubmitQuoteButton(modifier = Modifier.fillMaxWidth())
            OutlinedActionButton("My Submissions", Icons.Outlined.Inbox, modifier = Modifier.fillMaxWidth(), onClick = onOpenSubmissions)
        }
    }
}

@Composable
private fun NewestQuotes(quotes: List<Quote>, failed: Boolean) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Newest quotes")
        PaletteCard {
            when {
                quotes.isEmpty() && failed -> Text(
                    "Couldn't load the newest quotes. Check your connection and try again.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.text,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                quotes.isEmpty() -> repeat(3) { index ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = palette.hairline)
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(palette.text.copy(alpha = 0.10f)))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.55f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(palette.text.copy(alpha = 0.10f)))
                }
                else -> quotes.forEachIndexed { index, quote ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = palette.hairline)
                    Text(quote.text, fontFamily = QuoteSerif, fontSize = 17.sp, lineHeight = 24.sp, color = palette.text)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        quote.approvedDate?.let {
                            Text(
                                "Added ${formatDate(it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryText,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        if (isAuthorValid(quote.author)) {
                            Text(
                                "— ${quote.author.orEmpty().trim()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent,
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
