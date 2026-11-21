package com.danielagapov.quotedroplet.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielagapov.quotedroplet.data.Quote
import com.danielagapov.quotedroplet.data.highlightRanges
import com.danielagapov.quotedroplet.data.isAuthorValid
import com.danielagapov.quotedroplet.data.quoteWithAuthor
import com.danielagapov.quotedroplet.ui.LocalAppContainer
import com.danielagapov.quotedroplet.ui.theme.LocalPalette

const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.danielagapov.quotedroplet"

/** A quote with like, save, copy and share buttons (SingleQuoteView in the iOS app). */
@Composable
fun QuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier,
    highlight: String = "",
    onAuthorClick: ((String) -> Unit)? = null,
    // Custom quotes show edit and delete in place of like and save
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val container = LocalAppContainer.current
    val palette = LocalPalette.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val likedIds by container.localQuotes.likedIds.collectAsStateWithLifecycle()
    val bookmarkIds by container.localQuotes.bookmarkIds.collectAsStateWithLifecycle()
    val likeCounts by container.repository.likeCounts.collectAsStateWithLifecycle()
    val liked = quote.id in likedIds
    val bookmarked = quote.id in bookmarkIds
    val likes = likeCounts[quote.id] ?: quote.likes ?: 0
    val heartScale by animateFloatAsState(
        targetValue = if (liked) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
        label = "heart",
    )
    val highlightColor = palette.accent.copy(alpha = 0.3f)
    // Custom quotes' authors have no author page
    val authorClick = if (quote.isCustom) null else onAuthorClick

    PaletteCard(modifier, padding = 20.dp) {
        Icon(
            Icons.Filled.FormatQuote,
            contentDescription = null,
            tint = palette.accent.copy(alpha = 0.7f),
            modifier = Modifier.size(26.dp).padding(bottom = 2.dp),
        )
        Text(
            text = highlighted(quote.text, highlight, highlightColor),
            fontFamily = QuoteSerif,
            fontSize = 20.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Normal,
            color = palette.text,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (isAuthorValid(quote.author)) {
            val author = quote.author.orEmpty()
            val authorText = buildAnnotatedString {
                append("— ")
                append(highlighted(author.trim(), highlight, highlightColor))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f, fill = false)
                        .then(if (authorClick != null) Modifier.clickable { authorClick(author) } else Modifier),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = authorText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                        // Long attributions wrap instead of being cut off
                        maxLines = 3,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (authorClick != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = palette.accent.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp).height(1.dp).background(palette.hairline))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (quote.isCustom) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = palette.secondaryText, modifier = Modifier.size(21.dp))
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = palette.secondaryText, modifier = Modifier.size(21.dp))
                    }
                }
            } else {
                IconButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    container.toggleLike(quote)
                }) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (liked) "Unlike" else "Like",
                        tint = if (liked) palette.accent else palette.secondaryText,
                        modifier = Modifier.size(22.dp).scale(heartScale),
                    )
                }
                Text(
                    "$likes",
                    color = if (liked) palette.accent else palette.secondaryText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.offset(x = (-6).dp),
                )
                IconButton(onClick = { container.toggleBookmark(quote) }) {
                    Icon(
                        imageVector = if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (bookmarked) "Remove from saved" else "Save",
                        tint = if (bookmarked) palette.accent else palette.secondaryText,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            IconButton(onClick = {
                copyQuote(context, quote)
                container.recordInteraction()
            }) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = palette.secondaryText, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { shareQuote(context, quote) }) {
                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = palette.secondaryText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

fun highlighted(text: String, query: String, color: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    highlightRanges(text, query).forEach { range ->
        addStyle(SpanStyle(background = color), range.first, range.last + 1)
    }
}

fun copyQuote(context: Context, quote: Quote) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Quote", quoteWithAuthor(quote)))
    // Android 13+ shows its own "copied" confirmation
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Quote copied", Toast.LENGTH_SHORT).show()
    }
}

fun shareQuote(context: Context, quote: Quote) {
    val author = if (isAuthorValid(quote.author)) "\n— ${quote.author.orEmpty().trim()}" else ""
    val text = "From the Quote Droplet app:\n\n\"${quote.text.trim()}\"$author\n\n$PLAY_STORE_URL"
    shareText(context, text, "Share quote")
}

fun shareText(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, title))
}
