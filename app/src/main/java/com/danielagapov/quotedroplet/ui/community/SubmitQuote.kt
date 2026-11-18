package com.danielagapov.quotedroplet.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.data.SubmitOutcome
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.PaletteTextField
import com.danielagapov.quotedroplet.ui.components.PickerRow
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.launch

class SubmitQuoteViewModel(private val container: AppContainer, prefillAuthor: String?) : ViewModel() {
    var text by mutableStateOf("")
    var author by mutableStateOf(prefillAuthor.orEmpty())
    // Remembered between submissions and launches, so users don't retype their name each time
    var submitterName by mutableStateOf(container.settings.submitterName)
        private set
    var category by mutableStateOf(QuoteCategory.All)
    var submitting by mutableStateOf(false)
        private set
    var resultMessage by mutableStateOf<String?>(null)
        private set
    private val defaultAuthor = prefillAuthor.orEmpty()

    fun updateSubmitterName(value: String) {
        submitterName = value
        container.settings.submitterName = value
    }

    fun submit() {
        if (text.isBlank() || submitting) return
        submitting = true
        viewModelScope.launch {
            val outcome = container.repository.submitQuote(
                text = text,
                author = author,
                category = category,
                submitterName = submitterName,
                submitterId = container.submitterId,
            )
            if (outcome != SubmitOutcome.Failed) {
                text = ""
                author = defaultAuthor
                category = QuoteCategory.All
            }
            resultMessage = outcome.message
            submitting = false
        }
    }

    fun dismissResult() {
        resultMessage = null
    }
}

/** "Submit a Quote" button that opens the submission sheet (SubmitView in the iOS app). */
@Composable
fun SubmitQuoteButton(prefillAuthor: String? = null, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val vm: SubmitQuoteViewModel = viewModel(key = "submit-${prefillAuthor.orEmpty()}") {
        SubmitQuoteViewModel(container, prefillAuthor)
    }
    var open by rememberSaveable { mutableStateOf(false) }
    val result = vm.resultMessage

    OutlinedActionButton("Submit a Quote", Icons.AutoMirrored.Outlined.Send, modifier = modifier, onClick = { open = true })

    LaunchedEffect(result) {
        if (result != null) open = false
    }
    if (open) SubmitQuoteSheet(vm, onDismiss = { open = false })
    if (result != null) {
        AlertDialog(
            onDismissRequest = vm::dismissResult,
            confirmButton = { TextButton(onClick = vm::dismissResult) { Text("OK") } },
            title = { Text("Submission Received") },
            text = { Text(result) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitQuoteSheet(vm: SubmitQuoteViewModel, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    var showInfo by rememberSaveable { mutableStateOf(false) }

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
            TextButton(onClick = { showInfo = true }) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = palette.accent)
                Spacer(Modifier.size(6.dp))
                Text("How This Works", color = palette.accent)
            }
            Text(
                "Submit a Quote",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
            )
            PaletteTextField(vm.text, { vm.text = it }, "Quote Text", minLines = 3)
            PaletteTextField(vm.author, { vm.author = it }, "Quote Author")
            PaletteTextField(vm.submitterName, vm::updateSubmitterName, "Your Name/Alias (Optional)")
            PickerRow(
                label = "Quote Category",
                options = QuoteCategory.submittable,
                selected = vm.category,
                onSelect = { vm.category = it },
                optionText = { if (it == QuoteCategory.All) "Unsure" else it.displayName },
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = vm::submit,
                enabled = vm.text.isNotBlank() && !vm.submitting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.background,
                    disabledContainerColor = palette.accent.copy(alpha = 0.35f),
                    disabledContentColor = palette.background.copy(alpha = 0.8f),
                ),
            ) {
                if (vm.submitting) CircularProgressIndicator(Modifier.size(22.dp), color = palette.background, strokeWidth = 2.dp)
                else Text("Submit", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            title = { Text("How Quote Submission Works") },
            text = {
                Text(
                    "Submitted quotes will either be approved to be added into the app's quote database, or dismissed.\n\n" +
                        "I'll be able to edit typos or insert missing fields, such as author and classification, so don't worry about this when submitting."
                )
            },
        )
    }
}
