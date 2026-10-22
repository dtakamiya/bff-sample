package com.example.bff.webclient.presentation

import com.example.bff.webclient.application.usecase.GetDashboardUseCase
import com.example.bff.webclient.domain.exception.UserNotFoundException
import com.example.bff.webclient.domain.model.UserId
import com.example.bff.webclient.presentation.dto.DashboardResponse
import com.example.bff.webclient.presentation.dto.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ダッシュボード API（GET /api/dashboard/{userId}）。presentation 層の入口（HTTP の変換のみを担う）。
 * 「MVC + WebClient + block()」方式の要点がこの 1 関数に集中している。
 *
 * 役割は 3 つ。パス変数を値オブジェクトにする、ユースケースを呼ぶ、結果を画面向けの JSON 形へ写す。
 * 業務の判断（並列化・失敗の優先順位）はユースケースに任せる。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val getDashboardUseCase: GetDashboardUseCase,
) {
    /**
     * 同期メソッドとして結果を返す。内部の Mono は block() で待つため、
     * 応答が完成するまで、このリクエストを処理しているサーブレットのワーカースレッドが占有される。
     *
     * - 下流の並列化の利得は得られるが、待ち時間の間スレッドは何もせず占有されたままになる。
     *   同時リクエスト数はスレッド数に制約される。これが WebFlux 版との本質的な違い。
     * - block() は Netty のイベントループなど非ブロッキングのスレッド上で呼ぶと例外になる。この関数は MVC スレッドで動く前提。
     * - 下流の失敗（UserNotFoundException / DownstreamFailureException）は block() がそのまま再送出するので、
     *   BffExceptionHandler で 404 / 502 に変換される。
     */
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): DashboardResponse {
        // block() は並列処理が完了するまで MVC スレッドを待たせる。各呼び出しは timeout で打ち切られる。
        // 下流の空応答はアダプタで 502 になるため、ここで null になるのは不変条件違反のみ（その場合は 500）。
        val result = requireNotNull(getDashboardUseCase.execute(toUserId(userId)).block()) {
            "empty dashboard for $userId"
        }
        return result.toResponse()
    }

    /**
     * パス変数を UserId にする。空白のみの ID は値オブジェクトを作れないため、利用者が指定したユーザーが存在しないものとして扱い 404 にする。
     * 例外の変換はここだけで行い、ユースケース以下は UserId を前提のまま保つ。
     */
    private fun toUserId(rawUserId: String): UserId =
        runCatching { UserId(rawUserId) }.getOrElse { throw UserNotFoundException(rawUserId) }
}
