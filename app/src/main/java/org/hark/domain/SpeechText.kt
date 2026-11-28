package org.hark.domain

object SpeechText {
    const val MAX_CHARS = 600
    const val LINK = "attached link"

    /**
     * Web addresses: anything starting with a scheme or "www.", or a bare domain with a common ending
     * (example.com, bit.ly/abc). Trailing sentence punctuation is left out of the match.
     */
    private val url = Regex(
        """(?i)\b(?:(?:https?|ftp)://|www\.)[^\s<>"]+[^\s<>".,;:!?')\]]""" +
            """|(?i)\b(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+""" +
            """(?:com|org|net|edu|gov|io|co|us|uk|ca|de|me|ly|gl|app|dev|info|biz|tv|ai|be|to|link)\b""" +
            """(?:/[^\s<>"]*[^\s<>".,;:!?')\]])?/?""",
    )

    /** Replaces every web address in [text] with "attached link". */
    fun replaceLinks(text: String): String = url.replace(text, LINK)

    fun format(m: IncomingMessage): String {
        val text = m.text?.let(::replaceLinks)
        val body = when {
            text == null -> "sent an attachment."
            text.length > MAX_CHARS -> truncate(text) + " … message truncated."
            else -> text
        }
        val lead = when {
            m.fromTitleOnly && m.sender.contains("voicemail", ignoreCase = true) -> m.sender
            else -> "Message from ${m.sender}"
        }
        return if (text == null) "$lead $body" else "$lead: $body"
    }

    private fun truncate(text: String): String {
        val cut = text.substring(0, MAX_CHARS)
        val space = cut.lastIndexOf(' ')
        return (if (space > MAX_CHARS / 2) cut.substring(0, space) else cut).trimEnd()
    }
}
