package com.danielagapov.quotedroplet.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.danielagapov.quotedroplet.data.QuoteCategory
import com.danielagapov.quotedroplet.ui.theme.LocalPalette

/** Quotes and titles are set in the serif face, like the iOS app. */
val QuoteSerif: FontFamily = FontFamily.Serif

val CardShape = RoundedCornerShape(22.dp)
val ControlShape = RoundedCornerShape(16.dp)

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

/**
 * Title row of each tab: a small accent eyebrow over a large serif title, with the droplet button
 * that opens App Info (the iOS header).
 */
@Composable
fun HeaderBar(title: String, onOpenInfo: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val palette = LocalPalette.current
    Row(
        modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    subtitle.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = palette.accent,
                )
            }
            Text(
                title,
                fontFamily = QuoteSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(palette.card)
                .border(1.dp, palette.hairline, CircleShape)
                .clickable(onClick = onOpenInfo),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.WaterDrop, contentDescription = "App info", tint = palette.accent, modifier = Modifier.size(22.dp))
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
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(palette.card).border(1.dp, palette.hairline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = palette.text, modifier = Modifier.size(20.dp))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

/** Large serif title inside a screen (author name, page headings). */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start, eyebrow: String? = null) {
    val palette = LocalPalette.current
    Column(modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        if (eyebrow != null) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                color = palette.accent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
            )
        }
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontFamily = QuoteSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            color = palette.text,
            textAlign = textAlign,
        )
    }
}

/** Small uppercase label above a group of settings or a list. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = LocalPalette.current.secondaryText,
    )
}

@Composable
fun MessageText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = LocalPalette.current.secondaryText,
        textAlign = TextAlign.Center,
    )
}

/** Icon in a soft accent circle, a title and a message: for empty lists. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    PaletteCard(modifier.padding(top = 16.dp), padding = 28.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontFamily = QuoteSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = palette.text)
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LocalPalette.current.accent)
    }
}

/** Stacked action buttons share one width so they line up. */
fun Modifier.actionButtonWidth(): Modifier = widthIn(max = 340.dp).fillMaxWidth()

/** Call-to-action button: filled with the accent ([primary]) or a raised card. */
@Composable
fun ActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val content = if (primary) palette.onAccent else palette.text
    Row(
        modifier
            .clip(ControlShape)
            .background(if (primary) palette.accent else palette.raised)
            .then(if (primary) Modifier else Modifier.border(1.dp, palette.hairline, ControlShape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
    }
}

/** Secondary call-to-action (kept under its old name). */
@Composable
fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = ActionButton(text, icon, onClick, modifier, primary = false, enabled = enabled)

/** Accent-tinted rounded square holding a row's icon. */
@Composable
fun IconTile(icon: ImageVector) {
    val palette = LocalPalette.current
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(palette.accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
    }
}

/** An icon + title row inside a card; [trailing] holds a value, switch or chevron. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = palette.text)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.secondaryText)
            }
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 66.dp).height(1.dp).background(LocalPalette.current.hairline))
}

/** A labelled dropdown row (iOS menu-style picker). Draws its own card unless [inCard]. */
@Composable
fun <T> PickerRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionText: (T) -> String,
    modifier: Modifier = Modifier,
    optionFont: (T) -> FontFamily? = { null },
    icon: ImageVector? = null,
    inCard: Boolean = false,
) {
    val palette = LocalPalette.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (inCard) Modifier
                else Modifier.clip(ControlShape).background(palette.card).border(1.dp, palette.hairline, ControlShape)
            )
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = if (icon != null) 12.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconTile(icon)
            Spacer(Modifier.width(14.dp))
        }
        Text(label.removeSuffix(":"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = palette.text)
        Spacer(Modifier.weight(1f).width(8.dp))
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    optionText(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = palette.accent,
                    fontFamily = optionFont(selected),
                    textAlign = TextAlign.End,
                )
                Icon(Icons.Filled.UnfoldMore, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
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

/** Pill-shaped segmented control in the palette's colors. */
@Composable
fun PaletteSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(palette.card)
            .border(1.dp, palette.hairline, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val container by animateColorAsState(if (selected) palette.accent else Color.Transparent, label = "segment")
            val content by animateColorAsState(if (selected) palette.onAccent else palette.secondaryText, label = "segmentText")
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(container)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = content, maxLines = 1)
            }
        }
    }
}

/** Horizontally scrolling category chips (Search and Top). */
@Composable
fun CategoryChips(
    categories: List<QuoteCategory>,
    selected: QuoteCategory,
    onSelect: (QuoteCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { item ->
            val isSelected = item == selected
            Text(
                text = item.displayName,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) palette.accent else palette.card)
                    .then(if (isSelected) Modifier else Modifier.border(1.dp, palette.hairline, CircleShape))
                    .clickable { onSelect(item) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (isSelected) palette.onAccent else palette.text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** The rounded surface quotes and settings groups sit on (QuotesSectionOuterStyling on iOS). */
@Composable
fun PaletteCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        modifier
            .fillMaxWidth()
            .shadow(if (palette.isDark) 6.dp else 3.dp, CardShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CardShape)
            .background(palette.card)
            .border(1.dp, palette.hairline, CardShape)
            .padding(padding),
        content = content,
    )
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
        shape = ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.hairline,
            focusedLabelColor = palette.accent,
            unfocusedLabelColor = palette.secondaryText,
            cursorColor = palette.accent,
            focusedContainerColor = palette.card,
            unfocusedContainerColor = palette.card,
        ),
    )
}

/** Placeholder quote cards (loading). */
@Composable
fun SkeletonQuoteCard(seed: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.16f,
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
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.text.copy(alpha = pulse))
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxWidth(widths[3])
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accent.copy(alpha = pulse * 1.6f))
            )
        }
    }
}
