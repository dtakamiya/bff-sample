package com.example.bff.webflux.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * 下流 API 用の WebClient Bean を組み立てる。
 *
 * 依存関係の注意: WebClient.Builder の自動設定は spring-boot-starter-webflux ではなく
 * spring-boot-starter-webclient 側にある（build.gradle.kts 参照）。このスターターが無いと
 * Builder の Bean が見つからず、起動時に失敗する。
 *
 * Builder を直接 WebClient.create() せず Boot の Builder を使う理由: Boot の Builder には
 * 自動設定（Jackson と Kotlin モジュールを含むコーデック、カスタマイザ）が適用済みで、
 * それを引き継げるため。
 *
 * baseUrl を Builder に入れておくと、DownstreamClient は "/users/{id}" のような相対パスだけを書ける。
 * タイムアウトはここでは設定せず、DownstreamClient の各 Mono に timeout を掛けている。
 */
@Configuration
class WebClientConfig {

    @Bean
    fun downstreamWebClient(
        builder: WebClient.Builder,
        properties: DownstreamProperties,
    ): WebClient = builder.baseUrl(properties.baseUrl).build()
}
