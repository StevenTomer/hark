package org.hark.domain

/** Turns a notification snapshot into the incoming messages it carries. Ignores non-message notifications. */
object MessageExtractor {
    private val ignoredCategories = setOf(
        "call", "missed_call", "progress", "service", "status", "err", "sys", "transport", "alarm", "reminder", "event",
    )

    private val senderPrefix = Regex("""([^:\n]{1,40}):\s+(.+)""", RegexOption.DOT_MATCHES_ALL)

    fun extract(n: NotificationSnapshot): List<IncomingMessage> {
        if (n.isGroupSummary || n.isOngoing) return emptyList()
        val style = n.styleMessages
        if (style != null) {
            val conversation = n.conversationTitle?.takeIf { n.isGroupConversation && it.isNotBlank() }
            return style.filter { !it.fromSelf && !it.sender.isNullOrBlank() && (it.text == null || it.text.isNotBlank()) }
                .map { IncomingMessage(it.sender!!.trim(), it.text?.trim(), conversation, it.timestamp) }
        }
        if (n.category in ignoredCategories) return emptyList()
        val title = n.title?.trim().orEmpty()
        val text = n.text?.trim().orEmpty()
        if (title.isEmpty() || text.isEmpty()) return emptyList()
        val time = if (n.whenTime > 0) n.whenTime else n.postTime
        // A group shown without MessagingStyle: the title lists the participants and the text is "Bob: hi".
        // Read only the actual sender.
        if (title.contains(',') || title.contains(" & ")) {
            senderPrefix.matchEntire(text)?.let { match ->
                return listOf(IncomingMessage(match.groupValues[1].trim(), match.groupValues[2].trim(), null, time))
            }
        }
        return listOf(IncomingMessage(title, text, null, time, fromTitleOnly = true))
    }
}
