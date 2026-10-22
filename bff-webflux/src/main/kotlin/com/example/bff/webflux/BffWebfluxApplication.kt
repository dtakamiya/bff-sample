package com.example.bff.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * WebFlux（Netty）版 BFF のエントリポイント。
 *
 * spring-boot-starter-webflux を使うため、サーブレットコンテナ（Tomcat）ではなく Netty 上で動く。
 * 少数のイベントループで多数の接続を捌くので、ハンドラがスレッドを占有しないことが前提になる
 * （詳細は DashboardController の KDoc）。
 *
 * @ConfigurationPropertiesScan は、このパッケージ配下の @ConfigurationProperties（DownstreamProperties）を
 * Bean として登録する。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class BffWebfluxApplication

fun main(args: Array<String>) {
    runApplication<BffWebfluxApplication>(*args)
}
