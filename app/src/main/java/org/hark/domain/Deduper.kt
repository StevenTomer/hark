package org.hark.domain

/**
 * Messaging apps repost a conversation's notification whenever it changes (new message, read on another
 * device, reply sent). Remembers what has been seen per notification key so each message is read once.
 * Messages are marked seen whether or not they end up spoken, so nothing piles up while Bluetooth is off.
 */
class Deduper(private val freshnessMillis: Long = 5 * 60_000L, private val capacity: Int = 200) {
    private data class Seen(val lastTimestamp: Long, val textHashes: Set<Int>)

    private val seen = object : LinkedHashMap<String, Seen>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Seen>?) = size > capacity
    }

    @Synchronized
    fun filterNew(key: String, messages: List<IncomingMessage>, now: Long): List<IncomingMessage> {
        if (messages.isEmpty()) return emptyList()
        val previous = seen[key]
        val fresh = messages.filter { now - it.timestamp <= freshnessMillis }
        val result = fresh.filter { msg ->
            val hash = hashOf(msg)
            when {
                previous == null -> true
                hash in previous.textHashes -> false
                msg.fromTitleOnly -> true
                else -> msg.timestamp > previous.lastTimestamp
            }
        }
        val lastTimestamp = maxOf(previous?.lastTimestamp ?: Long.MIN_VALUE, messages.maxOf { it.timestamp })
        val hashes = ((previous?.textHashes ?: emptySet()) + messages.map(::hashOf)).toList().takeLast(50).toSet()
        seen[key] = Seen(lastTimestamp, hashes)
        return result
    }

    // Title-only notifications often refresh their timestamp when reposted, so they are keyed on content alone.
    private fun hashOf(m: IncomingMessage) =
        (m.sender + "\u0000" + m.text + if (m.fromTitleOnly) "" else "\u0000" + m.timestamp).hashCode()
}
