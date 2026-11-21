package com.danielagapov.quotedroplet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.danielagapov.quotedroplet.ui.theme.LocalPalette

/**
 * An author's name ("— Name ›") that opens their author page. Used wherever an author is shown
 * outside the full quote card, so every author in the app leads to their quotes.
 */
@Composable
fun AuthorLink(
    author: String,
    onOpenAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.End,
) {
    val palette = LocalPalette.current
    Row(
        modifier.clickable(onClickLabel = "More quotes by ${author.trim()}") { onOpenAuthor(author) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "— ${author.trim()}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
            // Long attributions ("Lord Krishna, Bhagavad Gita") wrap instead of being cut off
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.accent.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
    }
}
