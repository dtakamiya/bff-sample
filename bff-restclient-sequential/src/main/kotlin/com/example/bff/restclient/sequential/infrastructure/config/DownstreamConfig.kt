package com.example.bff.restclient.sequential.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

/**
 * 下流向け RestClient の組み立て（infrastructure 層）。
 *
 * 並列版との違いは、スレッドプール（ExecutorService）を持たないことだけ。
 * タイムアウトの設定は両版で同じになっている。
 */
@Configuration(proxyBeanMethods = false)
class DownstreamConfig {

    /**
     * 下流 3 エンドポイント共通の RestClient。
     *
     * タイムアウトは [DownstreamProperties.timeoutMs] を 2 箇所に使う:
     * - 接続確立の上限（HttpClient の connectTimeout）
     * - 応答待ちの上限（JDK 版リクエストファクトリの readTimeout）
     *
     * 逐次版では 1 回の呼び出しに待てる時間の目安がこの値になり、全呼び出しの待ちが足し算になる。
     * 接続と応答待ちは別々に効くため、1 回の呼び出しが理論上最大 2 倍まで伸びる点には注意。
     */
    @Bean
    fun downstreamRestClient(
        builder: RestClient.Builder,
        properties: DownstreamProperties,
    ): RestClient {
        val timeout = Duration.ofMillis(properties.timeoutMs)
        val requestFactory = JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(timeout).build(),
        ).apply { setReadTimeout(timeout) }
        return builder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
