package com.example.bff.stub.support

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

const val DEFAULT_DELAY_MS = 200L
const val UNKNOWN_ID = "unknown"

/**
 * Applies the injected latency and failure behavior for a stub request.
 *
 * Order: latency first (applies to error responses too), then forced failure (500),
 * then unknown-id lookup (404).
 */
fun applyStubBehavior(
    id: String,
    fail: Boolean,
    delayMsOverride: Long?,
    configuredDelayMs: Long,
) {
    Thread.sleep(resolveDelayMs(delayMsOverride, configuredDelayMs))
    if (fail) {
        throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "injected failure")
    }
    if (id == UNKNOWN_ID) {
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "resource not found: $id")
    }
}

private fun resolveDelayMs(delayMsOverride: Long?, configuredDelayMs: Long): Long {
    if (delayMsOverride == null) {
        return configuredDelayMs
    }
    if (delayMsOverride < 0) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "delayMs must be >= 0")
    }
    return delayMsOverride
}
