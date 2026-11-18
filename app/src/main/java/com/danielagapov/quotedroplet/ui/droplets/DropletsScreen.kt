package com.danielagapov.quotedroplet.ui.droplets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.LoadingIndicator
import com.danielagapov.quotedroplet.ui.components.MessageText
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.PageTitle
import com.danielagapov.quotedroplet.ui.components.PaletteSegmentedControl
import com.danielagapov.quotedroplet.ui.components.QuoteCard
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DropletsViewModel(private val container: AppContainer) : ViewModel() {
    private val _feed = MutableStateFlow<List<Quote>>(emptyList())
    val feed: StateFlow<List<Quote>> = _feed.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()
    // Set when a page load brought nothing new (offline, or only repeats)
    private val _stalled = MutableStateFlow(false)
    val stalled: StateFlow<Boolean> = _stalled.asStateFlow()

    fun onAppear() {
        if (_feed.value.isEmpty()) loadMore()
    }

    fun loadMore() {
        if (_loading.value || _feed.value.size >= MAX_QUOTES) return
        _loading.value = true
        viewModelScope.launch {
            appendPage()
            _loading.value = false
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        _refreshing.value = true
        viewModelScope.launch {
            _feed.value = emptyList()
            appendPage()
            _refreshing.value = false
        }
    }

    private suspend fun appendPage() {
        val results = List(PAGE_SIZE) {
            viewModelScope.async { container.repository.randomQuote(QuoteCategory.All).getOrNull() }
        }.awaitAll().filterNotNull()
        val before = _feed.value
        val added = results.distinctBy { it.id }.filter { r -> before.none { it.id == r.id } }
        _feed.value = (before + added).take(MAX_QUOTES)
        _stalled.value = added.isEmpty()
    }

    fun refreshCounts(quotes: List<Quote>) = container.repository.refreshLikeCounts(quotes.map { it.id })

    companion object {
        const val PAGE_SIZE = 5
        const val MAX_QUOTES = 15
    }
}

@Composable
fun DropletsScreen(onOpenInfo: () -> Unit, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: DropletsViewModel = viewModel { DropletsViewModel(container) }
    val pagerState = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    val openRecent by container.openRecentRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.onAppear() }
    LaunchedEffect(openRecent) {
        if (openRecent) {
            pagerState.scrollToPage(2)
            container.openRecentRequests.value = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        HeaderBar(onOpenInfo)
        PaletteSegmentedControl(
            labels = listOf("Feed", "Saved", "Recent"),
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // Swipe between Feed, Saved and Recent, like the iOS swipe gesture
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> FeedPage(vm, onOpenAuthor)
                1 -> SavedPage(vm, onOpenAuthor)
                else -> RecentPage(vm, onOpenAuthor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedPage(vm: DropletsViewModel, onOpenAuthor: (String) -> Unit) {
    val quotes by vm.feed.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val stalled by vm.stalled.collectAsStateWithLifecycle()

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = vm::refresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { PageTitle("Quotes Feed") }
            if (quotes.isEmpty()) {
                item {
                    when {
                        loading || refreshing -> MessageText("Loading Quotes Feed...")
                        stalled -> MessageText("Couldn't load quotes. Check your connection, then pull down to try again.")
                    }
                }
            }
            items(quotes, key = { it.id }) { quote ->
                QuoteCard(quote, onAuthorClick = onOpenAuthor)
            }
            if (quotes.isNotEmpty()) {
                item(key = "footer") {
                    when {
                        quotes.size >= DropletsViewModel.MAX_QUOTES ->
                            MessageText("You've reached the quote limit of ${DropletsViewModel.MAX_QUOTES}. Maybe take a break?")
                        stalled && !loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            OutlinedActionButton("Load More", Icons.Filled.Refresh, onClick = vm::loadMore)
                        }
                        else -> {
                            // Reaching the bottom loads the next 5 quotes
                            LaunchedEffect(quotes.size) { vm.loadMore() }
                            LoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPage(vm: DropletsViewModel, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val palette = LocalPalette.current
    val bookmarks by container.localQuotes.bookmarks.collectAsStateWithLifecycle()
    val quotes = remember(bookmarks) { bookmarks.asReversed() }
    LaunchedEffect(Unit) { vm.refreshCounts(quotes) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { PageTitle("Saved Quotes") }
        if (quotes.isEmpty()) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MessageText("You have no saved quotes.\n\nPlease save some from the Quotes Feed by pressing this:")
                    Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = palette.accent, modifier = Modifier.size(34.dp))
                }
            }
        }
        items(quotes, key = { it.id }) { quote ->
            QuoteCard(quote, onAuthorClick = onOpenAuthor)
        }
    }
}

@Composable
private fun RecentPage(vm: DropletsViewModel, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val palette = LocalPalette.current
    val recents by container.localQuotes.recents.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshCounts(recents) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PageTitle("Recent Quotes") }
        if (recents.isEmpty()) {
            item {
                MessageText(
                    "You have no recent quotes.\n\nTurn on notifications or add the Quote Droplet widget to your home screen to see them listed here."
                )
            }
        } else {
            item {
                Text(
                    "Your most recent quotes from notifications and your widget.",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                    textAlign = TextAlign.Center,
                )
            }
        }
        items(recents, key = { it.id }) { quote ->
            QuoteCard(quote, onAuthorClick = onOpenAuthor)
        }
    }
}
