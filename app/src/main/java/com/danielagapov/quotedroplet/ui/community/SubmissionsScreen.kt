package com.danielagapov.quotedroplet.ui.community

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.Submission
import com.danielagapov.quotedroplet.data.SubmissionStatus
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.BackTopBar
import com.danielagapov.quotedroplet.ui.components.PageTitle
import com.danielagapov.quotedroplet.ui.components.PaletteCard
import com.danielagapov.quotedroplet.ui.components.ScreenBackground
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubmissionsViewModel(private val container: AppContainer) : ViewModel() {
    private val _submissions = MutableStateFlow<List<Submission>>(emptyList())
    val submissions: StateFlow<List<Submission>> = _submissions.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            container.repository.submissions(container.submitterId)
                .onSuccess {
                    _submissions.value = it
                    _error.value = null
                }
                .onFailure { _error.value = "Couldn't load your submissions. Pull down to try again." }
            _loading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SubmissionsViewModel = viewModel { SubmissionsViewModel(container) }
    val submissions by vm.submissions.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val palette = LocalPalette.current
    LaunchedEffect(Unit) { vm.load() }

    ScreenBackground {
        Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(onBack) }) { padding ->
            PullToRefreshBox(
                isRefreshing = loading && submissions.isNotEmpty(),
                onRefresh = vm::load,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        PaletteCard {
                            PageTitle("My Submissions")
                            if (submissions.isEmpty()) {
                                Text(
                                    text = when {
                                        loading -> "Loading Submissions ..."
                                        error != null -> error.orEmpty()
                                        else -> "You haven't submitted any quotes from this device yet. Quotes you submit will show up here along with their approval status."
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.text,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                )
                            }
                            submissions.forEachIndexed { index, submission ->
                                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = palette.accent.copy(alpha = 0.4f))
                                SubmissionRow(submission)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionRow(submission: Submission) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(submission.text, style = MaterialTheme.typography.titleLarge, color = palette.text)
        if (isAuthorValid(submission.author)) {
            Text(
                "— ${submission.author.orEmpty().trim()}",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.accent,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusBadge(submission.submissionStatus)
            Spacer(Modifier.weight(1f))
            submission.submittedDate?.let {
                Text(formatDate(it), style = MaterialTheme.typography.bodySmall, color = palette.text)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SubmissionStatus) {
    val palette = LocalPalette.current
    val icon = when (status) {
        SubmissionStatus.Pending -> Icons.Outlined.Schedule
        SubmissionStatus.Approved -> Icons.Outlined.CheckCircle
        SubmissionStatus.Rejected -> Icons.Outlined.Cancel
    }
    Row(
        Modifier
            .border(1.5.dp, palette.accent, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
        Text(status.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.accent)
    }
}
