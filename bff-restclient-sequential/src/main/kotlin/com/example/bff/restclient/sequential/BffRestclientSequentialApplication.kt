package com.example.bff.restclient.sequential

import com.example.bff.restclient.sequential.config.DownstreamProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

/**
 * 逐次版 BFF のエントリポイント。
 *
 * このモジュールは「スレッドを一切使わず、リクエストスレッドの上で下流を 1 つずつ呼ぶ」比較サンプル。
 * 並列版 (bff-restclient) と同じ API・同じ下流を相手にするので、差分は集約方式だけになるよう作ってある。
 * 並列版にある `downstream.parallelism` のようなワーカー設定は、ここには存在しない。
 */
@SpringBootApplication
// downstream.* (base-url / timeout-ms) を DownstreamProperties に束ねる。値は application.yml 由来。
@EnableConfigurationProperties(DownstreamProperties::class)
class BffRestclientSequentialApplication

fun main(args: Array<String>) {
    runApplication<BffRestclientSequentialApplication>(*args)
}
