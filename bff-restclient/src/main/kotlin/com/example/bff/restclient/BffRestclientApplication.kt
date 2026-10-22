package com.example.bff.restclient

import com.example.bff.restclient.config.DownstreamProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

/**
 * 「MVC + RestClient（ブロッキング）+ 固定スレッドプール + CompletableFuture」方式の BFF。
 *
 * 同じ集約処理を別方式で実装した比較サンプルの一つ（兄弟モジュール: bff-webclient / bff-webflux /
 * bff-restclient-sequential）。このモジュールを読むときは次の 3 箇所を中心に見ると全体がつかめる。
 * - 並列化の構造: [com.example.bff.restclient.service.DashboardService]
 * - 下流呼び出しとタイムアウト: [com.example.bff.restclient.config.DownstreamConfig]
 * - 例外から HTTP ステータスへの変換: [com.example.bff.restclient.api.GlobalExceptionHandler]
 *
 * 起動クラスがルートパッケージにあるため、配下のサブパッケージの Bean はすべてスキャン対象になる。
 */
@SpringBootApplication
// DownstreamProperties は @Component ではないため、この登録がないと Bean として注入できない。
@EnableConfigurationProperties(DownstreamProperties::class)
class BffRestclientApplication

fun main(args: Array<String>) {
    runApplication<BffRestclientApplication>(*args)
}
