package com.danielagapov.quotedroplet.ui.author

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.community.SubmitQuoteButton
import com.danielagapov.quotedroplet.ui.components.BackTopBar
import com.danielagapov.quotedroplet.ui.components.LoadingIndicator
import com.danielagapov.quotedroplet.ui.components.MessageText
import com.danielagapov.quotedroplet.ui.components.PageTitle
import com.danielagapov.quotedroplet.ui.components.QuoteCard
import com.danielagapov.quotedroplet.ui.components.ScreenBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthorViewModel(private val container: AppContainer, private val author: String) : ViewModel() {
    private val _quotes = MutableStateFlow<List<Quote>?>(null)
    val quotes: StateFlow<List<Quote>?> = _quotes.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun load() {
        if (_quotes.value != null) return
        viewModelScope.launch {
            container.repository.quotesByAuthor(author)
                .onSuccess { _quotes.value = it.distinctBy { q -> q.id }.take(MAX_QUOTES) }
                .onFailure { _failed.value = true }
        }
    }

    private companion object {
        const val MAX_QUOTES = 200
    }
}

/** Every quote by one author (AuthorView in the iOS app). */
@Composable
fun AuthorScreen(author: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: AuthorViewModel = viewModel(key = "author-$author") { AuthorViewModel(container, author) }
    val quotes by vm.quotes.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    LaunchedEffect(author) { vm.load() }

    ScreenBackground {
        Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(onBack) }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PageTitle("Quotes by $author:") }
                val list = quotes
                when {
                    failed -> item { MessageText("Couldn't load quotes by this author. Check your connection and try again.") }
                    list == null -> item { LoadingIndicator() }
                    else -> items(list, key = { it.id }) { quote -> QuoteCard(quote) }
                }
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        MessageText("Missing a quote from this author?\nI'd greatly appreciate submissions:")
                        Box(Modifier.padding(bottom = 16.dp)) { SubmitQuoteButton(prefillAuthor = author) }
                    }
                }
            }
        }
    }
}
