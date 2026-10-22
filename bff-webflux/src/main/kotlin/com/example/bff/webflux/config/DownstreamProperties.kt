package com.example.bff.webflux.config

import org.springframework.boot.context.properties.ConfigurationProperties

// 既定値。application.yml の downstream.* がある場合はそちらが優先される。
const val DEFAULT_BASE_URL = "http://localhost:9090"
// 1 回の下流呼び出しごとのタイムアウト（ミリ秒）。画面全体の締切ではない。
const val DEFAULT_TIMEOUT_MS = 2000L

/**
 * downstream.* の設定を束ねる。
 *
 * init の require で不正値を弾く。Spring のバインド時（起動時）に失敗させるため、
 * 不正な設定は起動時点で気づける。リクエストの途中で初めて失敗することはない。
 */
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
