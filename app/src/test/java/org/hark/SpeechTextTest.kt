package org.hark

import org.hark.domain.IncomingMessage
import org.hark.domain.SpeechText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {
    @Test fun formatsSenderAndText() =
        assertEquals("Message from Ann: hi", SpeechText.format(IncomingMessage("Ann", "hi", null, 0)))

    @Test fun groupReadsOnlyTheSender() =
        assertEquals("Message from Bob: dinner?", SpeechText.format(IncomingMessage("Bob", "dinner?", "Ann, Bob, Carol", 0)))

    @Test fun formatsAttachment() =
        assertEquals("Message from Bob sent an attachment.", SpeechText.format(IncomingMessage("Bob", null, null, 0)))

    @Test fun voicemailTitleIsNotPrefixed() =
        assertEquals("New voicemail from Ann: call me", SpeechText.format(IncomingMessage("New voicemail from Ann", "call me", null, 0, true)))

    @Test fun truncatesLongMessagesAtWordBoundary() {
        val out = SpeechText.format(IncomingMessage("Ann", "word ".repeat(300), null, 0))
        assertTrue(out.endsWith("message truncated."))
        assertTrue(out.length < SpeechText.MAX_CHARS + 60)
    }
}
