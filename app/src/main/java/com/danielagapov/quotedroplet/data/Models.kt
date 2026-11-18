package com.danielagapov.quotedroplet.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Serializable
data class Quote(
    val id: Int,
    val text: String,
    val author: String? = null,
    val classification: String? = null,
    val likes: Int? = null,
    // Only returned by some endpoints (e.g. quotes/recent)
    @SerialName("approved_at") val approvedAt: String? = null,
) {
    val approvedDate: Instant? get() = parseApiDate(approvedAt)
}

/** A quote the user submitted, as returned by GET /quotes/submissions. */
@Serializable
data class Submission(
    val id: Int,
    val text: String,
    val author: String? = null,
    val classification: String? = null,
    val likes: Int? = null,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
) {
    val submissionStatus: SubmissionStatus get() = SubmissionStatus.from(status)
    val submittedDate: Instant? get() = parseApiDate(createdAt)
}

enum class SubmissionStatus(val displayName: String) {
    Pending("Awaiting Approval"),
    Approved("Approved"),
    Rejected("Not Approved");

    companion object {
        fun from(raw: String): SubmissionStatus = when (raw.lowercase()) {
            "approved" -> Approved
            "rejected" -> Rejected
            else -> Pending
        }
    }
}

@Serializable
data class CountResponse(val count: Int = 0)

@Serializable
data class NewQuoteRequest(
    val text: String,
    val author: String? = null,
    val classification: String,
    @SerialName("submitter_name") val submitterName: String? = null,
    // Lets the user look this submission up later in My Submissions
    @SerialName("submitter_id") val submitterId: String,
)

enum class QuoteCategory(val displayName: String) {
    All("All"),
    Wisdom("Wisdom"),
    Motivation("Motivation"),
    Discipline("Discipline"),
    Philosophy("Philosophy"),
    Inspiration("Inspiration"),
    Upliftment("Upliftment"),
    Love("Love"),
    Saved("Saved");

    val apiName: String get() = displayName.lowercase()

    /** The value for the backend's classification/category filters (null = no filter). */
    val apiFilter: String? get() = if (this == All) null else apiName

    companion object {
        fun fromKey(key: String?): QuoteCategory = entries.firstOrNull { it.name == key } ?: All

        /** Categories the backend knows about (Saved only exists on the device). */
        val remote: List<QuoteCategory> get() = entries.filter { it != Saved }

        /** Choices when submitting a quote: All is shown as "Unsure". */
        val submittable: List<QuoteCategory> get() = entries.filter { it != Saved }
    }
}

enum class QuoteFrequency(private val label: String, val hours: Long) {
    EightHours("8 hours", 8),
    TwelveHours("12 hours", 12),
    OneDay("day", 24),
    TwoDays("2 days", 48),
    FourDays("4 days", 96),
    OneWeek("week", 168);

    val displayName: String get() = "Every $label"

    companion object {
        fun fromKey(key: String?): QuoteFrequency = entries.firstOrNull { it.name == key } ?: OneDay
    }
}

enum class FeedbackType(val displayName: String, val apiType: String) {
    General("General", "general"),
    Bug("Bug Report", "bug"),
    Feature("Feature Request", "feature"),
    Content("Content", "general"),
}

/** Mirrors isAuthorValid in the iOS app and backend: hides placeholder authors. */
fun isAuthorValid(author: String?): Boolean =
    author != null && author.isNotBlank() && author != "Unknown Author" && author != "NULL"

/** Parses the backend's RFC 3339 timestamps, with or without fractional seconds. */
fun parseApiDate(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(value).toInstant()
    } catch (e: DateTimeParseException) {
        try {
            Instant.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

/** Every case-insensitive occurrence of [query] in [text], for search highlighting. */
fun highlightRanges(text: String, query: String): List<IntRange> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val ranges = mutableListOf<IntRange>()
    var start = text.indexOf(q, ignoreCase = true)
    while (start >= 0) {
        ranges += start until start + q.length
        start = text.indexOf(q, start + q.length, ignoreCase = true)
    }
    return ranges
}

/** What gets copied or shared: the quote, then "— Author" on its own line. */
fun quoteWithAuthor(quote: Quote): String =
    if (isAuthorValid(quote.author)) "${quote.text.trim()}\n— ${quote.author.orEmpty().trim()}" else quote.text.trim()
