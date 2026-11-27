package org.hark.domain

/**
 * A daily window, in minutes after midnight, when nothing is read aloud. The start is included, the end
 * is not. A window whose end is earlier than its start runs overnight (22:00–07:00). Equal times mean
 * no quiet time at all.
 */
data class QuietHours(val enabled: Boolean, val startMinute: Int, val endMinute: Int) {
    fun isQuiet(minuteOfDay: Int): Boolean = when {
        !enabled || startMinute == endMinute -> false
        startMinute < endMinute -> minuteOfDay in startMinute until endMinute
        else -> minuteOfDay >= startMinute || minuteOfDay < endMinute
    }

    companion object {
        const val DEFAULT_START = 22 * 60
        const val DEFAULT_END = 7 * 60
    }
}
