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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.danielagapov.quotedroplet.ui.components.ActionButton
import com.danielagapov.quotedroplet.ui.components.PaletteTextField
import com.danielagapov.quotedroplet.ui.components.QuoteSerif
import com.danielagapov.quotedroplet.widget.WidgetSync
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
import com.danielagapov.quotedroplet.ui.components.EmptyState
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.LoadingIndicator
import com.danielagapov.quotedroplet.ui.components.MessageText
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.PageTitle
import com.danielagapov.quotedroplet.ui.components.PaletteSegmentedControl
import com.danielagapov.quotedroplet.ui.components.QuoteCard
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    val openPage by container.openDropletsPageRequests.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.onAppear() }
    LaunchedEffect(openPage) {
        openPage?.let {
            pagerState.scrollToPage(it.coerceIn(0, 2))
            container.openDropletsPageRequests.value = null
        }
    }

    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d")) }
    Column(Modifier.fillMaxSize()) {
        HeaderBar("Droplets", onOpenInfo, subtitle = today)
        PaletteSegmentedControl(
            labels = listOf("Feed", "Saved", "Custom"),
            selectedIndex = pagerState.currentPage,
            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        // Swipe between Feed, Saved and Custom, like the iOS swipe gesture
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> FeedPage(vm, onOpenAuthor)
                1 -> SavedPage(vm, onOpenAuthor)
                else -> CustomPage()
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (quotes.isEmpty()) {
                item {
                    when {
                        loading || refreshing -> LoadingIndicator(Modifier.padding(top = 60.dp))
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (quotes.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.BookmarkBorder,
                    "Nothing saved yet",
                    "Tap the bookmark on any quote in your feed to keep it here.",
                )
            }
        }
        items(quotes, key = { it.id }) { quote ->
            QuoteCard(quote, onAuthorClick = onOpenAuthor)
        }
    }
}

/** The user's own quotes: add, edit and delete them, and show them in the widget and notifications. */
@Composable
private fun CustomPage() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val palette = LocalPalette.current
    val customs by container.localQuotes.customs.collectAsStateWithLifecycle()
    val category by container.settings.quoteCategory.collectAsStateWithLifecycle()
    // Newest first, so a quote you just added shows at the top
    val quotes = remember(customs) { customs.asReversed() }
    var editor by remember { mutableStateOf<CustomEditorTarget?>(null) }
    var toDelete by remember { mutableStateOf<Quote?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Add a Quote", Icons.Filled.Add, onClick = { editor = CustomEditorTarget(null) }, modifier = Modifier.fillMaxWidth())
                if (quotes.isNotEmpty()) {
                    if (category == QuoteCategory.Custom) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Text(
                                "Your widget and notifications show these quotes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.secondaryText,
                            )
                        }
                    } else {
                        ActionButton(
                            "Show These in My Widget",
                            Icons.Outlined.Widgets,
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                container.settings.setQuoteCategory(QuoteCategory.Custom)
                                WidgetSync.onCategoryChanged(context)
                            },
                        )
                    }
                }
            }
        }
        if (quotes.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    Icons.Outlined.FormatQuote,
                    "Your own quotes",
                    "Add quotes you wrote, heard from someone, or can't find in the app. They stay on this device, and your widget and notifications can show them.",
                    modifier = Modifier.padding(top = 0.dp),
                )
            }
        }
        items(quotes, key = { it.id }) { quote ->
            QuoteCard(
                quote,
                onEdit = { editor = CustomEditorTarget(quote) },
                onDelete = { toDelete = quote },
            )
        }
    }

    editor?.let { target ->
        CustomQuoteSheet(
            editing = target.quote,
            onDismiss = { editor = null },
            onSave = { text, author ->
                container.saveCustomQuote(text, author, target.quote)
                editor = null
            },
        )
    }
    toDelete?.let { quote ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this quote?") },
            text = { Text("It's only stored on this device, so it can't be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    container.deleteCustomQuote(quote)
                    toDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Which quote the editor is open for (null quote = adding a new one). */
private data class CustomEditorTarget(val quote: Quote?)

private const val CUSTOM_MAX_LENGTH = 500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomQuoteSheet(editing: Quote?, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val palette = LocalPalette.current
    var text by rememberSaveable { mutableStateOf(editing?.text.orEmpty()) }
    var author by rememberSaveable { mutableStateOf(editing?.author.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = palette.background,
        contentColor = palette.text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (editing == null) "New Quote" else "Edit Quote",
                fontFamily = QuoteSerif,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = palette.text,
            )
            Text(
                "Only stored on this device. Short quotes fit the widget best.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.secondaryText,
            )
            PaletteTextField(text, { text = it.take(CUSTOM_MAX_LENGTH) }, "Quote text", minLines = 3)
            PaletteTextField(author, { author = it }, "Author (optional)")
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onSave(text, author) },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                    disabledContainerColor = palette.accent.copy(alpha = 0.35f),
                    disabledContentColor = palette.onAccent.copy(alpha = 0.7f),
                ),
            ) {
                Text(if (editing == null) "Add Quote" else "Save Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}
