package com.danielagapov.quotedroplet.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.danielagapov.quotedroplet.ui.theme.LocalPalette

/** Screen background: the palette's first color, with a gentle vertical fade. */
@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundBottom))),
        content = content,
    )
}

/** Top bar of each tab: the ☰ button opens App Info, like the iOS header. */
@Composable
fun HeaderBar(onOpenInfo: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenInfo) {
            Icon(Icons.Filled.Menu, contentDescription = "App info", tint = palette.accent, modifier = Modifier.size(30.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(onBack: () -> Unit, title: String = "") {
    val palette = LocalPalette.current
    TopAppBar(
        title = { Text(title, color = palette.text, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.accent)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = LocalPalette.current.accent,
        textAlign = textAlign,
    )
}

@Composable
fun MessageText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(16.dp),
        style = MaterialTheme.typography.titleLarge,
        color = LocalPalette.current.accent,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalPalette.current.accent)
    }
}

/** Stacked action buttons share one width so they line up. */
fun Modifier.actionButtonWidth(): Modifier = widthIn(max = 340.dp).fillMaxWidth()

/** The iOS app's outlined "Submit a Quote"-style button. */
@Composable
fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .clip(shape)
            .border(2.dp, palette.accent, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.text)
        Icon(icon, contentDescription = null, tint = palette.text, modifier = Modifier.size(26.dp))
    }
}

/** A labelled picker in a bordered box (iOS menu-style picker). */
@Composable
fun <T> PickerRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionText: (T) -> String,
    modifier: Modifier = Modifier,
    optionFont: (T) -> FontFamily? = { null },
) {
    val palette = LocalPalette.current
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.background)
            .border(2.dp, palette.accent, shape)
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = palette.text)
        Spacer(Modifier.weight(1f).width(8.dp))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    optionText(selected),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                    fontFamily = optionFont(selected),
                    textAlign = TextAlign.End,
                )
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = palette.accent)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option), fontFamily = optionFont(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                        trailingIcon = if (option == selected) {
                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                        } else null,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaletteSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = palette.accent,
                    activeContentColor = palette.background,
                    activeBorderColor = palette.accent,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = palette.text,
                    inactiveBorderColor = palette.accent.copy(alpha = 0.7f),
                ),
                icon = {},
            ) {
                Text(label, maxLines = 1, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** A rounded card in the palette (QuotesSectionOuterStyling in the iOS app). */
@Composable
fun PaletteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.card, contentColor = palette.text),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun PaletteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val palette = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        singleLine = minLines == 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.accent.copy(alpha = 0.5f),
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.text.copy(alpha = 0.7f),
            cursorColor = palette.accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

/** Placeholder quote cards (search's empty state and loading). */
@Composable
fun SkeletonQuoteCard(seed: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    val widths = remember(seed) {
        val r = java.util.Random(seed.toLong())
        listOf(0.75f + r.nextFloat() * 0.2f, 0.3f + r.nextFloat() * 0.45f, 0.2f + r.nextFloat() * 0.4f, 0.3f + r.nextFloat() * 0.25f)
    }
    PaletteCard(modifier) {
        widths.take(3).forEach { w ->
            Box(
                Modifier
                    .fillMaxWidth(w)
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.text.copy(alpha = pulse))
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("— ", color = palette.accent)
            Box(
                Modifier
                    .fillMaxWidth(widths[3])
                    .height(11.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accent.copy(alpha = pulse))
            )
        }
    }
}
