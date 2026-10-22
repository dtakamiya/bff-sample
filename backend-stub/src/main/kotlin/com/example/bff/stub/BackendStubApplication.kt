package com.example.bff.stub

import com.example.bff.stub.config.StubProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

/**
 * BFF 比較サンプルの下流 API（backend-stub）の起動クラス。
 *
 * 3 つの BFF（bff-webclient / bff-restclient / bff-restclient-sequential）が共通で呼ぶ
 * ユーザー・注文・商品の API を、固定データでポート 9090 に提供する。
 * 本物のバックエンドの代わりに、遅延と障害を注入できるようにしてある。
 *
 * BFF は `delayMs` / `fail` のクエリを付けずに呼ぶ。BFF 比較で効く遅延は
 * `stub.delay-ms.*`（application.yml または起動引数）だけで、クエリは curl や k6 で
 * スタブを直接叩くときの手段になる。
 *
 * パッケージ直下に置いているので、component scan で controller 配下の `@RestController` が拾われる。
 * 各エンドポイントの仕様は [com.example.bff.stub.controller.UserController] と
 * [com.example.bff.stub.controller.ProductController]、共通の遅延・障害処理は
 * [com.example.bff.stub.support.applyStubBehavior] を参照。
 */
@SpringBootApplication
// stub.* の設定を StubProperties に束縛する。support 配下はトップレベルの関数と定数だけで Bean にはならない。
@EnableConfigurationProperties(StubProperties::class)
class BackendStubApplication

fun main(args: Array<String>) {
    // コマンドライン引数は Spring の環境設定として渡る（例: --stub.delay-ms.users=0 で既定遅延を変えられる）。
    runApplication<BackendStubApplication>(*args)
}
