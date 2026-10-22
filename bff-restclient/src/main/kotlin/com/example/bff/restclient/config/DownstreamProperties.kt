package com.example.bff.restclient.config

import org.springframework.boot.context.properties.ConfigurationProperties

const val DEFAULT_DOWNSTREAM_BASE_URL = "http://localhost:9090"
const val DEFAULT_DOWNSTREAM_TIMEOUT_MS = 2000L
const val DEFAULT_DOWNSTREAM_PARALLELISM = 32

@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_DOWNSTREAM_BASE_URL,
    val timeoutMs: Long = DEFAULT_DOWNSTREAM_TIMEOUT_MS,
    val parallelism: Int = DEFAULT_DOWNSTREAM_PARALLELISM,
)
