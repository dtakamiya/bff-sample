package com.example.bff.stub.config

import com.example.bff.stub.support.DEFAULT_DELAY_MS
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * スタブの振る舞いの設定（`stub.*`）。現状は応答遅延の既定値だけを持つ。
 *
 * ここの値は「クエリ `delayMs` が無いときの既定」にすぎない。クエリ指定があればそちらが優先される
 * （優先順位は [com.example.bff.stub.support.applyStubBehavior] の KDoc を参照）。
 * BFF はクエリを付けずに呼ぶため、BFF 比較で実際に効くのはこの設定値。
 * 値は application.yml で与え、テストプロファイル（application-test.yml）では 0 にして遅延をなくしている。
 */
@ConfigurationProperties(prefix = "stub")
data class StubProperties(
    // application.yml の stub.delay-ms.* に対応する。Spring の緩和バインディングにより、
    // YAML のケバブケース（delay-ms）がこのプロパティ名（delayMs）に結び付く。
    val delayMs: DelayMs = DelayMs(),
) {
    /**
     * エンドポイント種別ごとの既定遅延（ミリ秒）。
     *
     * 種別ごとに個別に指定できる。設定ファイルに値が無い項目は [DEFAULT_DELAY_MS] になる。
     */
    data class DelayMs(
        val users: Long = DEFAULT_DELAY_MS,
        val orders: Long = DEFAULT_DELAY_MS,
        val products: Long = DEFAULT_DELAY_MS,
    )
}
