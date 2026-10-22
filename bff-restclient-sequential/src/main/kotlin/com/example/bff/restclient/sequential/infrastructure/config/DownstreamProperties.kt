package com.example.bff.restclient.sequential.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** 下流の既定ベース URL。`application.yml` の `downstream.base-url` と揃えている。 */
const val DEFAULT_DOWNSTREAM_BASE_URL = "http://localhost:9090"

/**
 * 下流 1 呼び出しあたりのタイムアウト（ミリ秒）の既定値。`application.yml` と揃えている。
 *
 * 逐次版では全体の待ち時間が「呼び出し回数 × タイムアウト」で積み上がる。
 * ダッシュボードは user・orders・注文数ぶんの product で (2 + 注文数) 回呼ぶため、
 * 最悪ケースは (2 + 注文数) × このタイムアウト になる（1 回あたり timeout まで待った場合）。
 * 注文が多いユーザーほど応答が遅くなる点を、設定を変えるときに意識すること。
 */
const val DEFAULT_DOWNSTREAM_TIMEOUT_MS = 2000L

/**
 * `downstream.*` の設定値。
 *
 * [timeoutMs] の意味と使われ方は [DownstreamConfig] を参照。
 * 値を省略された場合の既定値は application.yml と同じにしてある（設定ファイルが欠けても同じ挙動になる）。
 */
@ConfigurationProperties(prefix = "downstream")
data class DownstreamProperties(
    val baseUrl: String = DEFAULT_DOWNSTREAM_BASE_URL,
    val timeoutMs: Long = DEFAULT_DOWNSTREAM_TIMEOUT_MS,
)
