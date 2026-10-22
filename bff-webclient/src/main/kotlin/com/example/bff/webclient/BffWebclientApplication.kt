package com.example.bff.webclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * bff-webclient のエントリポイント。
 *
 * この BFF は「MVC（サーブレット）+ WebClient」方式。リクエストは MVC のワーカースレッドで受け、
 * 下流への呼び出しは WebClient（Reactor の Mono/Flux）で並列化したうえで、コントローラが block() して同期的に返す。
 * リアクティブ化を最後まで貫く bff-webflux との比較材料として置いてある。
 *
 * @ConfigurationPropertiesScan により、@ConfigurationProperties を付けたクラス（DownstreamProperties など）が
 * このパッケージ配下から自動登録される。設定クラスを増やしても @Bean の登録は不要。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class BffWebclientApplication

fun main(args: Array<String>) {
    runApplication<BffWebclientApplication>(*args)
}
