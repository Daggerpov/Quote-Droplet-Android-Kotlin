package com.danielagapov.quotedroplet.ui.info

import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.sp
import com.danielagapov.quotedroplet.ui.components.PaletteCard
import com.danielagapov.quotedroplet.ui.components.QuoteSerif
import com.danielagapov.quotedroplet.ui.components.RowDivider
import com.danielagapov.quotedroplet.ui.components.SectionLabel
import com.danielagapov.quotedroplet.ui.components.SettingsRow
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielagapov.quotedroplet.AppContainer
import com.danielagapov.quotedroplet.BuildConfig
import com.danielagapov.quotedroplet.R
import com.danielagapov.quotedroplet.data.FeedbackType
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.components.BackTopBar
import com.danielagapov.quotedroplet.ui.components.OutlinedActionButton
import com.danielagapov.quotedroplet.ui.components.actionButtonWidth
import com.danielagapov.quotedroplet.ui.components.PLAY_STORE_URL
import com.danielagapov.quotedroplet.ui.components.PaletteTextField
import com.danielagapov.quotedroplet.ui.components.PickerRow
import com.danielagapov.quotedroplet.ui.components.ScreenBackground
import com.danielagapov.quotedroplet.ui.components.shareText
import com.danielagapov.quotedroplet.ui.theme.LocalPalette
import kotlinx.coroutines.launch

private const val APP_STORE_URL = "https://apps.apple.com/us/app/quote-droplet/id6455084603"

class FeedbackViewModel(private val container: AppContainer) : ViewModel() {
    var type by mutableStateOf(FeedbackType.General)
    var message by mutableStateOf("")
    var email by mutableStateOf("")
    var sending by mutableStateOf(false)
        private set
    var resultMessage by mutableStateOf<String?>(null)
        private set

    fun submit() {
        if (message.isBlank() || sending) return
        sending = true
        viewModelScope.launch {
            val ok = container.repository.sendFeedback(type, message, email, BuildConfig.VERSION_NAME)
            resultMessage = if (ok) {
                message = ""
                email = ""
                type = FeedbackType.General
                "Thanks for your feedback! I appreciate you taking the time to help improve Quote Droplet."
            } else {
                "Your feedback couldn't be sent. Please check your connection and try again."
            }
            sending = false
        }
    }

    fun dismissResult() {
        resultMessage = null
    }
}

@Composable
fun InfoScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val palette = LocalPalette.current
    val feedback: FeedbackViewModel = viewModel { FeedbackViewModel(container) }
    var showFeedback by rememberSaveable { mutableStateOf(false) }
    var showAppleNote by rememberSaveable { mutableStateOf(false) }

    ScreenBackground {
        Scaffold(containerColor = Color.Transparent, topBar = { BackTopBar(onBack) }) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        Modifier
                            .size(76.dp)
                            .shadow(8.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(Brush.linearGradient(listOf(palette.accent, lerp(palette.accent, Color.Black, 0.18f)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Quote Droplet", fontFamily = QuoteSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = palette.text)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText)
                }
                PaletteCard(padding = 0.dp) {
                    SettingsRow(Icons.Outlined.Share, "Share Quote Droplet", onClick = {
                        shareText(context, "Check out this app, Quote Droplet.\n\n$PLAY_STORE_URL", "Share Quote Droplet")
                    }) { Chevron() }
                    RowDivider()
                    SettingsRow(Icons.Outlined.StarBorder, "Rate Quote Droplet", onClick = { openPlayListing(context) }) { Chevron() }
                    RowDivider()
                    SettingsRow(Icons.Outlined.Feedback, "Send Feedback", subtitle = "Bugs, ideas or quote corrections", onClick = { showFeedback = true }) { Chevron() }
                    RowDivider()
                    SettingsRow(Icons.Outlined.Devices, "Also on iPhone & Mac", onClick = { showAppleNote = true }) { Chevron() }
                }
                SectionLabel("Find me online")
                AboutMeRow()
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.accent,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { openUrl(context, BuildConfig.PRIVACY_POLICY_URL) }.padding(8.dp),
                )
            }
        }
    }

    if (showFeedback) FeedbackSheet(feedback, onDismiss = { showFeedback = false })
    feedback.resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = feedback::dismissResult,
            confirmButton = { TextButton(onClick = feedback::dismissResult) { Text("OK") } },
            title = { Text("Feedback Received") },
            text = { Text(message) },
        )
    }
    if (showAppleNote) {
        AlertDialog(
            onDismissRequest = { showAppleNote = false },
            confirmButton = {
                TextButton(onClick = {
                    showAppleNote = false
                    openUrl(context, APP_STORE_URL)
                }) { Text("Open App Store") }
            },
            dismissButton = { TextButton(onClick = { showAppleNote = false }) { Text("OK") } },
            title = { Text("Also on iPhone, iPad & Mac") },
            text = {
                Text(
                    "Quote Droplet is on the App Store too, with the same quotes and widget. " +
                        "The Mac version shows quotes from a small icon in your menu bar, even offline."
                )
            },
        )
    }
}

@Composable
private fun Chevron() {
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = LocalPalette.current.secondaryText)
}

@Composable
private fun AboutMeRow() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(if (palette.isDark) 6.dp else 3.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(palette.card)
            .border(1.dp, palette.hairline, RoundedCornerShape(22.dp))
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinkLogo(R.drawable.logo_instagram, "Instagram") { openUrl(context, "https://www.instagram.com/quote_droplet_app") }
        LinkLogo(R.drawable.logo_linkedin, "LinkedIn") { openUrl(context, "https://www.linkedin.com/in/danielagapov/") }
        LinkLogo(R.drawable.logo_github, "GitHub") { openUrl(context, "https://github.com/Daggerpov") }
        LinkLogo(R.drawable.logo_gmail, "Email", width = 42) {
            openUrl(context, "mailto:danielagapov1@gmail.com?subject=Quote%20Droplet%20Contact")
        }
    }
}

@Composable
private fun LinkLogo(res: Int, description: String, width: Int = 34, onClick: () -> Unit) {
    Box(
        Modifier
            .size(54.dp)
            .shadow(3.dp, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(res), contentDescription = description, modifier = Modifier.size(width = width.dp, height = 34.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackSheet(vm: FeedbackViewModel, onDismiss: () -> Unit) {
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
                Text("About Feedback", color = palette.accent)
            }
            Text("Send Feedback", fontFamily = QuoteSerif, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = palette.text)
            PickerRow(
                label = "Feedback Type",
                options = FeedbackType.entries,
                selected = vm.type,
                onSelect = { vm.type = it },
                optionText = { it.displayName },
            )
            PaletteTextField(vm.message, { vm.message = it }, "Feedback Message", minLines = 5)
            PaletteTextField(vm.email, { vm.email = it }, "Email (Optional)", keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    vm.submit()
                    onDismiss()
                },
                enabled = vm.message.isNotBlank() && !vm.sending,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                    disabledContainerColor = palette.accent.copy(alpha = 0.35f),
                    disabledContentColor = palette.onAccent.copy(alpha = 0.7f),
                ),
            ) {
                if (vm.sending) CircularProgressIndicator(Modifier.size(22.dp), color = palette.onAccent, strokeWidth = 2.dp)
                else Text("Submit Feedback", fontWeight = FontWeight.Bold)
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            confirmButton = { TextButton(onClick = { showInfo = false }) { Text("OK") } },
            title = { Text("About Feedback") },
            text = {
                Text("Your feedback helps me improve Quote Droplet. I personally review all feedback and take it into consideration for future updates.")
            },
        )
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No app can open it (e.g. no email app); nothing else to do
    }
}

private fun openPlayListing(context: Context) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                .setPackage("com.android.vending")
        )
    } catch (e: ActivityNotFoundException) {
        openUrl(context, PLAY_STORE_URL)
    }
}
