package org.hark.domain

object SpeechText {
    const val MAX_CHARS = 600

    fun format(m: IncomingMessage): String {
        val body = when {
            m.text == null -> "sent an attachment."
            m.text.length > MAX_CHARS -> truncate(m.text) + " … message truncated."
            else -> m.text
        }
        val lead = when {
            m.fromTitleOnly && m.sender.contains("voicemail", ignoreCase = true) -> m.sender
            m.conversation != null -> "Message from ${m.sender} in ${m.conversation}"
            else -> "Message from ${m.sender}"
        }
        return if (m.text == null) "$lead $body" else "$lead: $body"
    }

    private fun truncate(text: String): String {
        val cut = text.substring(0, MAX_CHARS)
        val space = cut.lastIndexOf(' ')
        return (if (space > MAX_CHARS / 2) cut.substring(0, space) else cut).trimEnd()
    }
}
