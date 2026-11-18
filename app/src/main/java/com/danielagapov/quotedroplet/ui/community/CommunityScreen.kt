package com.danielagapov.quotedroplet.ui.community

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
        HeaderBar(onOpenInfo)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NewestQuotes(newest, failed)
            SubmitQuoteButton(modifier = Modifier.actionButtonWidth())
            OutlinedActionButton("My Submissions", Icons.Outlined.Inbox, modifier = Modifier.actionButtonWidth(), onClick = onOpenSubmissions)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NewestQuotes(quotes: List<Quote>, failed: Boolean) {
    val palette = LocalPalette.current
    PaletteCard {
        PageTitle("Newest Quotes")
        when {
            quotes.isEmpty() && failed -> Text(
                "Couldn't load the newest quotes. Check your connection and try again.",
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            quotes.isEmpty() -> {
                Text("Loading Quotes ...", style = MaterialTheme.typography.titleLarge, color = palette.text)
                repeat(3) {
                    Text("Quote Loading", style = MaterialTheme.typography.titleLarge, color = palette.text.copy(alpha = 0.5f), modifier = Modifier.padding(top = 10.dp))
                    Text(
                        "— Author Loading",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.accent.copy(alpha = 0.6f),
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            else -> quotes.forEachIndexed { index, quote ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = palette.accent.copy(alpha = 0.3f))
                Text(quote.text, style = MaterialTheme.typography.titleLarge, color = palette.text)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    quote.approvedDate?.let {
                        Text(
                            "Added ${formatDate(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.text.copy(alpha = 0.8f),
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (isAuthorValid(quote.author)) {
                        Text(
                            "— ${quote.author.orEmpty().trim()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.accent,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
