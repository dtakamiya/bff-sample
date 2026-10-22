package com.example.bff.stub.config

import com.example.bff.stub.support.DEFAULT_DELAY_MS
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "stub")
data class StubProperties(
    val delayMs: DelayMs = DelayMs(),
) {
    data class DelayMs(
        val users: Long = DEFAULT_DELAY_MS,
        val orders: Long = DEFAULT_DELAY_MS,
        val products: Long = DEFAULT_DELAY_MS,
    )
}
