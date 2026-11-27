package org.hark

import org.hark.domain.Deduper
import org.hark.domain.IncomingMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduperTest {
    private val now = 10_000_000L
    private fun msg(text: String, t: Long, titleOnly: Boolean = false) = IncomingMessage("Ann", text, null, t, titleOnly)

    @Test fun repostIsNotRespoken() {
        val d = Deduper()
        val first = listOf(msg("hi", now - 10))
        assertEquals(1, d.filterNew("k", first, now).size)
        assertTrue(d.filterNew("k", first, now + 5).isEmpty())
    }

    @Test fun newMessageInSameThreadIsSpoken() {
        val d = Deduper()
        d.filterNew("k", listOf(msg("hi", now - 10)), now)
        val next = d.filterNew("k", listOf(msg("hi", now - 10), msg("there", now)), now)
        assertEquals(listOf("there"), next.map { it.text })
    }

    @Test fun staleMessagesAreIgnored() {
        val d = Deduper()
        val out = d.filterNew("k", listOf(msg("old", now - 60 * 60_000), msg("new", now - 1_000)), now)
        assertEquals(listOf("new"), out.map { it.text })
    }

    @Test fun titleOnlyRepostWithNewTimestampIsNotRespoken() {
        val d = Deduper()
        assertEquals(1, d.filterNew("k", listOf(msg("voicemail", now - 10, true)), now).size)
        assertTrue(d.filterNew("k", listOf(msg("voicemail", now, true)), now).isEmpty())
        assertEquals(1, d.filterNew("k", listOf(msg("another", now, true)), now).size)
    }
}
