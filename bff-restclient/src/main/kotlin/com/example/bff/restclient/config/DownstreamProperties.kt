package com.example.bff.restclient.config

import org.springframework.boot.context.properties.ConfigurationProperties

// 設定が無い場合の既定値。application.yml にも同じ値が書かれており、設定ファイルの値が優先される。
const val DEFAULT_DOWNSTREAM_BASE_URL = "http://localhost:9090"
// 下流 1 呼び出しあたりのタイムアウト（ミリ秒）。接続と読み取りの両方に使う。
const val DEFAULT_DOWNSTREAM_TIMEOUT_MS = 2000L
// 並列呼び出し用スレッドプールのスレッド数。全リクエストで共有される同時実行数の上限。
const val DEFAULT_DOWNSTREAM_PARALLELISM = 32

/**
 * `downstream.*` の設定値を型安全に束ねる。
 *
 * - [baseUrl]: 下流 API のベース URL。`/users/...` などのパスはこれに続けて組み立てられる。
 * - [timeoutMs]: 下流 1 呼び出しあたりの上限。リクエスト全体の上限ではない。段数に応じて全体の待ち時間は伸びる（[DownstreamConfig] 参照）。
 * - [parallelism]: 増やすと、同時に下流へ飛ぶリクエストも増える。下流側の許容量と合わせて決める。
 */
@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_DOWNSTREAM_BASE_URL,
    val timeoutMs: Long = DEFAULT_DOWNSTREAM_TIMEOUT_MS,
    val parallelism: Int = DEFAULT_DOWNSTREAM_PARALLELISM,
)
