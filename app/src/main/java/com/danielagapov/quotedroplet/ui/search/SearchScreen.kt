package com.danielagapov.quotedroplet.ui.search

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
        if (category == QuoteCategory.Saved) {
            // Saved quotes live on the device, so search them locally
            _state.value = SearchState(results = container.localQuotes.bookmarks.value.asReversed().filter { matches(it, q) })
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
fun SearchScreen(onOpenAuthor: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm: SearchViewModel = viewModel { SearchViewModel(container) }
    val query by vm.query.collectAsStateWithLifecycle()
    val category by vm.category.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text(
                "Quote Search",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            TextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by quote text or author name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = palette.text) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = vm::clear) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear search", tint = palette.accent)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(26.dp),
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
                    focusedPlaceholderColor = palette.text.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = palette.text.copy(alpha = 0.6f),
                ),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(QuoteCategory.entries) { item ->
                val selected = item == category
                Text(
                    text = item.displayName,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) palette.text else palette.accent)
                        .clickable { vm.setCategory(item) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = palette.background,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val results = state.results
            when {
                query.isBlank() -> items(8) { index -> SkeletonQuoteCard(seed = index) }
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
