package org.hark.service

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.hark.appContainer
import org.hark.domain.MessageExtractor
import org.hark.domain.NotificationSnapshot
import org.hark.domain.SpeechText
import org.hark.domain.Sources
import org.hark.domain.StyleMessage

/** Watches message notifications from the SMS app and Google Voice and hands new ones to the speaker. */
class HarkListenerService : NotificationListenerService() {
    private val main = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val container = appContainer
        val settings = container.settings.settings.value
        if (!settings.enabled || sbn.packageName == packageName) return
        val source = Sources.sourceOf(sbn.packageName, Telephony.Sms.getDefaultSmsPackage(this)) ?: return
        if (!settings.reads(source)) return
        val snapshot = runCatching { snapshot(sbn) }.getOrNull() ?: return
        val messages = MessageExtractor.extract(snapshot)
        val fresh = container.deduper.filterNew(snapshot.key, messages, System.currentTimeMillis())
        if (fresh.isEmpty()) return
        main.post { fresh.forEach { container.speaker.speak(SpeechText.format(it)) } }
    }

    private fun snapshot(sbn: StatusBarNotification): NotificationSnapshot {
        val n = sbn.notification
        val extras = n.extras
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val user = style?.user
        val messages = style?.messages?.map { m ->
            val person = m.person
            val self = person == null ||
                (user?.key != null && person.key == user.key) ||
                (person.key == null && user?.name != null && person.name?.toString() == user.name?.toString())
            StyleMessage(
                sender = person?.name?.toString(),
                text = m.text?.toString(),
                timestamp = m.timestamp,
                fromSelf = self,
            )
        }
        return NotificationSnapshot(
            key = sbn.key,
            packageName = sbn.packageName,
            postTime = sbn.postTime,
            category = n.category,
            isGroupSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = sbn.isOngoing,
            styleMessages = messages,
            conversationTitle = style?.conversationTitle?.toString(),
            isGroupConversation = style?.isGroupConversation == true,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString(),
            whenTime = n.`when`,
        )
    }
}
