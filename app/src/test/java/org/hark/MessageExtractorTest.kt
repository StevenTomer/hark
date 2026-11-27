package org.hark

import org.hark.domain.MessageExtractor
import org.hark.domain.NotificationSnapshot
import org.hark.domain.StyleMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

fun snapshot(
    style: List<StyleMessage>? = null, title: String? = null, text: String? = null,
    summary: Boolean = false, ongoing: Boolean = false, category: String? = "msg",
    conversation: String? = null, group: Boolean = false, key: String = "k", whenTime: Long = 1_000,
) = NotificationSnapshot(key, "pkg", 1_000, category, summary, ongoing, style, conversation, group, title, text, whenTime)

class MessageExtractorTest {
    @Test fun messagingStyleSkipsSelfMessages() {
        val msgs = MessageExtractor.extract(snapshot(style = listOf(
            StyleMessage("Ann", "hi", 1, false),
            StyleMessage("Me", "hello", 2, true),
            StyleMessage("Ann", "how are you", 3, false),
        )))
        assertEquals(listOf("hi", "how are you"), msgs.map { it.text })
        assertEquals("Ann", msgs[0].sender)
    }

    @Test fun groupConversationIncludesTitle() {
        val m = MessageExtractor.extract(snapshot(style = listOf(StyleMessage("Bob", "dinner?", 1, false)),
            conversation = "Family", group = true)).single()
        assertEquals("Family", m.conversation)
    }

    @Test fun attachmentHasNullText() {
        val m = MessageExtractor.extract(snapshot(style = listOf(StyleMessage("Bob", null, 1, false)))).single()
        assertEquals(null, m.text)
    }

    @Test fun fallsBackToTitleAndText() {
        val m = MessageExtractor.extract(snapshot(title = "New voicemail from Ann", text = "Call me back")).single()
        assertEquals("New voicemail from Ann", m.sender)
        assertTrue(m.fromTitleOnly)
    }

    @Test fun titleListGroupUsesSenderFromText() {
        val m = MessageExtractor.extract(snapshot(title = "Ann, Bob, Carol", text = "Bob: running late")).single()
        assertEquals("Bob", m.sender)
        assertEquals("running late", m.text)
    }

    @Test fun colonInOneToOneTextIsKept() {
        val m = MessageExtractor.extract(snapshot(title = "Ann", text = "Note: bring chairs")).single()
        assertEquals("Ann", m.sender)
        assertEquals("Note: bring chairs", m.text)
    }

    @Test fun skipsSummariesOngoingAndCalls() {
        val style = listOf(StyleMessage("Ann", "hi", 1, false))
        assertTrue(MessageExtractor.extract(snapshot(style = style, summary = true)).isEmpty())
        assertTrue(MessageExtractor.extract(snapshot(style = style, ongoing = true)).isEmpty())
        assertTrue(MessageExtractor.extract(snapshot(title = "Missed call", text = "Ann", category = "missed_call")).isEmpty())
        assertTrue(MessageExtractor.extract(snapshot(title = "Messages", text = null)).isEmpty())
    }
}
