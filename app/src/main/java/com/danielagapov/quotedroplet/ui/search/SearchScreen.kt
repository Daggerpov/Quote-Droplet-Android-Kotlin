package com.danielagapov.quotedroplet.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import com.danielagapov.quotedroplet.ui.components.CategoryChips
import com.danielagapov.quotedroplet.ui.components.HeaderBar
import com.danielagapov.quotedroplet.ui.components.SectionLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.LoadingIndicator
import com.danielagapov.quotedroplet.ui.components.MessageText
import com.danielagapov.quotedroplet.ui.components.QuoteCard
import com.danielagapov.quotedroplet.ui.components.SkeletonQuoteCard
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class SearchState(
    val loading: Boolean = false,
    val results: List<Quote>? = null,
    val failed: Boolean = false,
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val container: AppContainer) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _category = MutableStateFlow(QuoteCategory.All)
    val category: StateFlow<QuoteCategory> = _category.asStateFlow()
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_query.debounce(300), _category) { q, c -> q.trim() to c }
                .distinctUntilChanged()
                .collectLatest { (q, c) -> search(q, c) }
        }
    }

    fun setQuery(value: String) {
        _query.value = value
        if (value.isBlank()) _state.value = SearchState()
    }

    fun setCategory(value: QuoteCategory) {
        _category.value = value
    }

    fun clear() = setQuery("")

    private suspend fun search(q: String, category: QuoteCategory) {
        if (q.isEmpty()) {
            _state.value = SearchState()
            return
        }
        _state.value = _state.value.copy(loading = true, failed = false)
        if (category.isOnDevice) {
            // Saved and Custom quotes live on the device, so search them locally
            val local = if (category == QuoteCategory.Custom) container.localQuotes.customs.value else container.localQuotes.bookmarks.value
            _state.value = SearchState(results = local.asReversed().filter { matches(it, q) })
            return
        }
        container.repository.search(q, category)
            .onSuccess { quotes -> _state.value = SearchState(results = quotes.filter { matches(it, q) }) }
            .onFailure { _state.value = SearchState(failed = true) }
    }

    // The backend matches text or author; keep only results that visibly contain the search
    private fun matches(quote: Quote, q: String): Boolean =
        quote.text.contains(q, ignoreCase = true) || quote.author?.contains(q, ignoreCase = true) == true
}

@Composable
fun SearchScreen(onOpenInfo: () -> Unit, onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel { SearchViewModel(container) }
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        HeaderBar("Search", onOpenInfo, subtitle = "Find a quote")
        TextField(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .border(1.dp, palette.hairline, RoundedCornerShape(16.dp)),
            placeholder = { Text("Quote text or author") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = palette.secondaryText) },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = vm::clear) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Clear search", tint = palette.secondaryText)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = palette.card,
                unfocusedContainerColor = palette.card,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent,
                focusedPlaceholderColor = palette.secondaryText,
                unfocusedPlaceholderColor = palette.secondaryText,
            ),
        )
        CategoryChips(
            categories = QuoteCategory.entries,
            selected = category,
            onSelect = vm::setCategory,
            modifier = Modifier.padding(vertical = 6.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val results = state.results
            when {
                query.isBlank() -> item { Suggestions(onPick = vm::setQuery) }
                state.failed -> item { MessageText("Couldn't search right now. Check your connection and try again.") }
                results == null -> item { LoadingIndicator() }
                results.isEmpty() && !state.loading -> item { MessageText("No quotes match “${query.trim()}”.") }
                else -> items(results, key = { it.id }) { quote ->
                    QuoteCard(quote, highlight = query, onAuthorClick = onOpenAuthor)
                }
            }
        }
    }
}

private val suggestions = listOf("Seneca", "courage", "Marcus Aurelius", "time", "kindness", "Maya Angelou")

/** Shown before typing: a few searches to try. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Try searching for")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { suggestion ->
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(palette.card)
                        .border(1.dp, palette.hairline, CircleShape)
                        .clickable { onPick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(suggestion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = palette.text)
                }
            }
        }
        Text(
            "Search matches words in a quote and in its author's name. Pick a category above to narrow it down.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.secondaryText,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}
