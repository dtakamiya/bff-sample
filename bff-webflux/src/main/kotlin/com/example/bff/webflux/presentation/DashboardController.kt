package com.example.bff.webflux.presentation

import com.example.bff.webflux.application.usecase.GetDashboardUseCase
import com.example.bff.webflux.domain.exception.UserNotFoundException
import com.example.bff.webflux.domain.model.UserId
import com.example.bff.webflux.presentation.dto.DashboardResponse
import com.example.bff.webflux.presentation.dto.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * ダッシュボード API（GET /api/dashboard/{userId}）の入口（WebFlux 版・presentation 層）。
 *
 * 役割は 3 つ。パス変数を値オブジェクトにする、ユースケースを呼ぶ、結果を画面向けの JSON 形へ写す。
 * 業務の判断（並列化・失敗の優先順位）はユースケースに任せる。
 *
 * ハンドラは Mono を返すだけで、結果を待たない。WebFlux がこの Mono を購読し、処理の完了を待つ間は
 * Netty のイベントループスレッドを解放して他の接続を処理する。応答は Mono が値を流したときに書き出される。
 * bff-webclient（MVC 版）はここで block() してリクエストスレッドを待たせるため、この点が両版の対照になる。
 *
 * 守るべきルール: このコントローラより下（application / infrastructure）でも、イベントループ上で
 * block()、Thread.sleep、JDBC、同期のファイル I/O などのブロッキング呼び出しをしない。
 * イベントループスレッドは CPU コア数程度しかなく、全接続で共有されるため、1 箇所でブロックすると
 * そのループに載っている全リクエストが止まる。Reactor もイベントループ上での block() を IllegalStateException で拒否する。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val getDashboardUseCase: GetDashboardUseCase,
) {
    /**
     * 利用者 ID のダッシュボードを返す。エラーは Mono の onError として流れ、
     * BffExceptionHandler が HTTP ステータスに変換する（ここでは try/catch しない）。
     */
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): Mono<DashboardResponse> =
        toUserId(userId)
            .flatMap { getDashboardUseCase.execute(it) }
            .map { result -> result.toResponse() }

    /**
     * パス変数を利用者 ID の値オブジェクトにする。空白だけの ID は UserId の検証で拒否されるため、
     * その IllegalArgumentException を「利用者が存在しない」（UserNotFoundException → 404）に変換する。
     * Mono.fromCallable で包むので、検証の失敗も Mono のエラーシグナルとして流れる。
     */
    private fun toUserId(rawUserId: String): Mono<UserId> =
        Mono.fromCallable { UserId(rawUserId) }
            .onErrorMap(IllegalArgumentException::class.java) { UserNotFoundException(rawUserId) }
}
