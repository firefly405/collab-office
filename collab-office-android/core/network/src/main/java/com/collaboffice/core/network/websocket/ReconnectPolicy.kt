package com.collaboffice.core.network.websocket

class ReconnectPolicy(
    private val maxDelay: Long = 30_000L,
    private val maxRetries: Int = 20
) {
    private var retryCount = 0

    fun nextDelay(): Long {
        if (retryCount >= maxRetries) return maxDelay
        val delay = minOf(1000L * (1 shl retryCount), maxDelay)
        retryCount++
        return delay
    }

    fun reset() { retryCount = 0 }
}
