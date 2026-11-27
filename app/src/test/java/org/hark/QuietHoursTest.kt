package org.hark

import org.hark.domain.QuietHours
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    private fun m(h: Int, min: Int = 0) = h * 60 + min

    @Test fun overnightWindow() {
        val q = QuietHours(true, m(22), m(7))
        assertTrue(q.isQuiet(m(22)))
        assertTrue(q.isQuiet(m(23, 59)))
        assertTrue(q.isQuiet(m(0)))
        assertTrue(q.isQuiet(m(6, 59)))
        assertFalse(q.isQuiet(m(7)))
        assertFalse(q.isQuiet(m(21, 59)))
        assertFalse(q.isQuiet(m(12)))
    }

    @Test fun sameDayWindow() {
        val q = QuietHours(true, m(13), m(14, 30))
        assertTrue(q.isQuiet(m(13)))
        assertTrue(q.isQuiet(m(14, 29)))
        assertFalse(q.isQuiet(m(14, 30)))
        assertFalse(q.isQuiet(m(12, 59)))
    }

    @Test fun disabledOrEmptyIsNeverQuiet() {
        assertFalse(QuietHours(false, m(22), m(7)).isQuiet(m(23)))
        assertFalse(QuietHours(true, m(8), m(8)).isQuiet(m(8)))
    }
}
