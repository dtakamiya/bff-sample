package com.example.bff.webclient.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 下流 API 専用の WebClient を 1 つ作る設定クラス。
 *
 * Spring Boot が用意する WebClient.Builder（prototype スコープで注入ごとに新しい Builder が来る）から
 * baseUrl を設定した WebClient を組み立てる。Bean 名は downstreamWebClient。
 * 接続・応答の個別タイムアウトはここでは設定しておらず、呼び出し全体の上限は DownstreamClient の timeout が担う。
 */
@Configuration
class WebClientConfig {

    /**
     * 下流用の WebClient Bean。DownstreamClient がコンストラクタで型により注入を受ける。
     * WebClient の Bean が 2 つ以上になると、DownstreamClient への注入が曖昧になり起動に失敗するので注意。
     */
    @Bean
    fun downstreamWebClient(
        builder: WebClient.Builder,
        properties: DownstreamProperties,
    ): WebClient = builder.baseUrl(properties.baseUrl).build()
}
