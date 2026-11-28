package org.hark

import org.hark.domain.IncomingMessage
import org.hark.domain.SpeechText
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkTest {
    private fun r(s: String) = SpeechText.replaceLinks(s)

    @Test fun schemeLinks() {
        assertEquals("look attached link", r("look https://example.com/a/b?c=1&d=2#x"))
        assertEquals("see attached link.", r("see http://foo.bar/baz."))
        assertEquals("(attached link)", r("(https://example.com/page)"))
    }

    @Test fun wwwAndBareDomains() {
        assertEquals("go to attached link now", r("go to www.example.org now"))
        assertEquals("attached link, then attached link!", r("bit.ly/3xYz, then maps.google.com/?q=pizza!"))
        assertEquals("try attached link", r("try Example.COM"))
    }

    @Test fun ordinaryTextIsUntouched() {
        listOf("e.g. this", "Note: bring chairs", "3.14 is pi", "Mr. Smith arrives at 5.30", "ok...see you", "file.txt")
            .forEach { assertEquals(it, r(it)) }
    }

    @Test fun messageThatIsOnlyALink() =
        assertEquals("Message from Bob: attached link",
            SpeechText.format(IncomingMessage("Bob", "https://youtu.be/dQw4w9WgXcQ", null, 0)))
}
