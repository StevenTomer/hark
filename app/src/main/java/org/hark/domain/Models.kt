package org.hark.domain

/** One message inside a conversation notification (from MessagingStyle). */
data class StyleMessage(
    val sender: String?,
    val text: String?,
    val timestamp: Long,
    val fromSelf: Boolean,
)

/** Android-free view of a posted notification, so extraction and dedupe can be unit-tested. */
data class NotificationSnapshot(
    val key: String,
    val packageName: String,
    val postTime: Long,
    val category: String?,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean,
    /** Null when the notification has no MessagingStyle. */
    val styleMessages: List<StyleMessage>?,
    val conversationTitle: String?,
    val isGroupConversation: Boolean,
    val title: String?,
    val text: String?,
    val whenTime: Long,
)

/** A new incoming message that should be read aloud. */
data class IncomingMessage(
    val sender: String,
    val text: String?,
    val conversation: String?,
    val timestamp: Long,
    /** True when the notification gave no structured sender, only a title (e.g. Voice voicemail). */
    val fromTitleOnly: Boolean = false,
)

enum class Source { SMS, VOICE }

object Sources {
    const val GOOGLE_MESSAGES = "com.google.android.apps.messaging"
    const val SAMSUNG_MESSAGES = "com.samsung.android.messaging"
    const val GOOGLE_VOICE = "com.google.android.apps.googlevoice"

    /** Which source a package belongs to, or null if Hark ignores it. */
    fun sourceOf(packageName: String, defaultSmsPackage: String?): Source? = when (packageName) {
        GOOGLE_VOICE -> Source.VOICE
        GOOGLE_MESSAGES, SAMSUNG_MESSAGES, defaultSmsPackage -> Source.SMS
        else -> null
    }
}
