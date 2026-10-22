package com.example.bff.webflux.web

import com.example.bff.webflux.model.DashboardResponse
import com.example.bff.webflux.service.DashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * ダッシュボード API の入口（WebFlux 版）。
 *
 * ハンドラは Mono を返すだけで、結果を待たない。WebFlux がこの Mono を購読し、
 * 処理の完了を待つ間は Netty のイベントループスレッドを解放して他の接続を処理する。
 * 応答は Mono が値を流したときに書き出される。bff-webclient（MVC 版）はここで block() して
 * リクエストスレッドを待たせるため、この点が両版の対照になる。
 *
 * 守るべきルール: このコントローラより下（Service / Client）でも、イベントループ上で
 * block()、Thread.sleep、JDBC、同期のファイル I/O などのブロッキング呼び出しをしない。
 * イベントループスレッドは CPU コア数程度しかなく、全接続で共有されるため、1 箇所でブロックすると
 * そのループに載っている全リクエストが止まる。Reactor もイベントループ上での block() を
 * IllegalStateException で拒否する。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    /**
     * 利用者 ID のダッシュボードを返す。エラーは Mono の onError として流れ、
     * BffExceptionHandler が HTTP ステータスに変換する（ここでは try/catch しない）。
     */
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): Mono<DashboardResponse> =
        dashboardService.fetchDashboard(userId)
}
