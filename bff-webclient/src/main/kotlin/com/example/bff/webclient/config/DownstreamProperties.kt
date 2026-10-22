package com.example.bff.webclient.config

import org.springframework.boot.context.properties.ConfigurationProperties

const val DEFAULT_BASE_URL = "http://localhost:9090"
const val DEFAULT_TIMEOUT_MS = 2000L

@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_BASE_URL,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    init {
        require(baseUrl.isNotBlank()) { "downstream.base-url must not be blank" }
        require(timeoutMs > 0) { "downstream.timeout-ms must be positive" }
    }
}
