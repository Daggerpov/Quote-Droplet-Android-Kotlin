package com.danielagapov.quotedroplet.ui.top

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.LoadingIndicator
import com.danielagapov.quotedroplet.ui.components.MessageText
import com.danielagapov.quotedroplet.ui.components.PickerRow
import com.danielagapov.quotedroplet.ui.components.copyQuote
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TopViewModel(private val container: AppContainer) : ViewModel() {
    private val _category = MutableStateFlow(QuoteCategory.All)
    val category: StateFlow<QuoteCategory> = _category.asStateFlow()
    private val _quotes = MutableStateFlow<List<Quote>>(emptyList())
    val quotes: StateFlow<List<Quote>> = _quotes.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private var job: Job? = null

    fun setCategory(category: QuoteCategory) {
        _category.value = category
        load()
    }

    fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            _loading.value = _quotes.value.isEmpty()
            _quotes.value = container.repository.topQuotes(_category.value).getOrNull().orEmpty()
            _loading.value = false
        }
    }
}

@Composable
fun TopScreen(onOpenInfo: () -> Unit, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: TopViewModel = viewModel { TopViewModel(container) }
    val category by vm.category.collectAsStateWithLifecycle()
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    // Reload whenever the tab is shown, like the iOS app
    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        HeaderBar(onOpenInfo)
        PickerRow(
            label = "Category:",
            options = QuoteCategory.remote,
            selected = category,
            onSelect = vm::setCategory,
            optionText = { it.displayName },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Box(Modifier.fillMaxSize()) {
            when {
                loading -> LoadingIndicator(Modifier.padding(top = 40.dp))
                quotes.isEmpty() -> MessageText("No quotes available for this category", Modifier.padding(top = 40.dp))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(quotes, key = { _, q -> q.id }) { index, quote ->
                        LeaderboardRow(rank = index + 1, quote = quote, onOpenAuthor = onOpenAuthor)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, quote: Quote, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val palette = LocalPalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val likedIds by container.localQuotes.likedIds.collectAsStateWithLifecycle()
    val likeCounts by container.repository.likeCounts.collectAsStateWithLifecycle()
    val liked = quote.id in likedIds

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card, contentColor = palette.text),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 6.dp, bottom = 4.dp)) {
            Text(
                "#$rank",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                modifier = Modifier.width(48.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(quote.text, style = MaterialTheme.typography.bodyLarge, color = palette.text, modifier = Modifier.padding(end = 10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAuthorValid(quote.author)) {
                        Text(
                            "— ${quote.author.orEmpty().trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = palette.accent,
                            modifier = Modifier.weight(1f).clickable { onOpenAuthor(quote.author.orEmpty()) },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    IconButton(onClick = {
                        copyQuote(context, quote)
                        container.recordInteraction()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = palette.accent, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        container.toggleLike(quote)
                    }) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (liked) "Unlike" else "Like",
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        "${likeCounts[quote.id] ?: quote.likes ?: 0}",
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
            }
        }
    }
}
