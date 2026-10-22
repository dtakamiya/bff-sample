package com.example.bff.webclient.web

import com.example.bff.webclient.model.DashboardResponse
import com.example.bff.webclient.service.DashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ダッシュボード API（GET /api/dashboard/{userId}）。
 * 「MVC + WebClient + block()」方式の要点がこの 1 関数に集中している。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
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
    fun getDashboard(@PathVariable("userId") userId: String): DashboardResponse =
        // block() は並列処理が完了するまで MVC スレッドを待たせる。各呼び出しは timeout で打ち切られる。
        // null が返るのは Mono が空で完了したとき（DashboardService.fetchDashboard の注意点参照）。その場合は 500 になる。
        requireNotNull(dashboardService.fetchDashboard(userId).block()) { "empty dashboard for $userId" }
}
