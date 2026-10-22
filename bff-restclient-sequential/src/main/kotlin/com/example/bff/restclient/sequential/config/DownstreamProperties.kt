package com.example.bff.restclient.sequential.config

import org.springframework.boot.context.properties.ConfigurationProperties

const val DEFAULT_DOWNSTREAM_BASE_URL = "http://localhost:9090"
const val DEFAULT_DOWNSTREAM_TIMEOUT_MS = 2000L

@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_DOWNSTREAM_BASE_URL,
    val timeoutMs: Long = DEFAULT_DOWNSTREAM_TIMEOUT_MS,
)
