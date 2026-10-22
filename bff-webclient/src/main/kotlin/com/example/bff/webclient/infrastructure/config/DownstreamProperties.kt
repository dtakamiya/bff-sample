package com.example.bff.webclient.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

// 既定値はローカルの backend-stub（ポート 9090）を指す。application.yml で上書きされる前提。
const val DEFAULT_BASE_URL = "http://localhost:9090"

// 1 回の下流呼び出しあたりの上限（ミリ秒）。全体の上限ではない。
// 呼び出しは 2 段（user/orders → products）あるため、ダッシュボード全体の最悪所要時間はおおむね 2 倍になる。
const val DEFAULT_TIMEOUT_MS = 2000L

/**
 * `downstream.*` の設定値（infrastructure 層の設定）。application.yml から読み込まれる（例: `downstream.base-url`、`downstream.timeout-ms`）。
 *
 * - 項目が無ければ上記の既定値を使う。
 * - init ブロックで不正値を弾く。コンストラクタバインディング時に例外となるため、設定ミスは
 *   最初のリクエストではなく起動時に表面化する。
 * - baseUrl は空白チェックのみで、URL としての妥当性は検査していない。
 */
@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_BASE_URL,
    val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
) {
    init {
        // 0 以下は timeout として意味をなさないため、設定時点で拒否する。
        require(baseUrl.isNotBlank()) { "downstream.base-url must not be blank" }
        require(timeoutMs > 0) { "downstream.timeout-ms must be positive" }
    }
}
