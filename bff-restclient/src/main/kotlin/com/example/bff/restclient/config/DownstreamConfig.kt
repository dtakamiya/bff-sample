package com.example.bff.restclient.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 下流呼び出しの基盤（RestClient と並列実行用スレッドプール）を Bean として定義する。
 * 設定値はすべて [DownstreamProperties] から読む。
 */
@Configuration(proxyBeanMethods = false)
class DownstreamConfig {

    /**
     * 下流 API 用の RestClient。Spring Boot が用意する [RestClient.Builder] を使い、
     * リクエストファクトリ（HTTP の実装とタイムアウト）だけを差し替える。
     * Builder 側の設定（メッセージコンバータ・Jackson の設定など）はそのまま引き継がれる。
     *
     * タイムアウトの掛かり方（どちらも `downstream.timeout-ms` の同じ値）:
     * - 接続確立までの上限（connectTimeout）。
     * - レスポンスが返るまでの上限（readTimeout）。
     * 超過すると [com.example.bff.restclient.client.DownstreamException] になり、利用者には 502 が返る。
     * 下流が止まっていても、スレッドは上限時間で解放される。
     *
     * 1 リクエストの待ち時間の目安は「段 1（user と orders の遅い方）＋段 2（product の遅い方）」で、
     * 最悪ケースは約 2 × timeout（プールの待ち時間を除く）。
     */
    @Bean
    fun downstreamRestClient(
        builder: RestClient.Builder,
        properties: DownstreamProperties,
    ): RestClient {
        val timeout = Duration.ofMillis(properties.timeoutMs)
        // connectTimeout は接続確立の上限。未指定だと、接続先が応答しないとき長く待ち続けてしまう。
        // setReadTimeout は JDK の HttpRequest.timeout として各リクエストに効き、主にレスポンスの到着までを制限する。
        // ボディ転送の途中で止まった場合にどこまで切れるかは未検証。
        val requestFactory = JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(timeout).build(),
        ).apply { setReadTimeout(timeout) }
        return builder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }

    /**
     * 並列呼び出し用の固定スレッドプール。
     *
     * - スレッド数は `downstream.parallelism`（既定 32）。**全リクエストで共有**される。
     *   つまり同時に走れる下流呼び出しの総数の上限で、1 リクエストが 32 本使えるわけではない。
     *   負荷が高いと、後続リクエストの呼び出しはキュー（上限なし）で待つ。
     * - 固定プールを使うのは Java 17 指定のため。仮想スレッド（Java 21 以降）を使う設計との比較点になる。
     * - 既定の ForkJoinPool.commonPool を使わない理由は [com.example.bff.restclient.service.DashboardService] を参照。
     * - 終了時は shutdown() で新規投入を止める。実行中のタスクの完了は待たない（awaitTermination はしていない）。
     */
    @Bean(destroyMethod = "shutdown")
    fun downstreamExecutor(properties: DownstreamProperties): ExecutorService =
        Executors.newFixedThreadPool(properties.parallelism)
}
