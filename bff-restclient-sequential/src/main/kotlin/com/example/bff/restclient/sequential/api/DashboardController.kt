package com.example.bff.restclient.sequential.api

import com.example.bff.restclient.sequential.service.DashboardService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ダッシュボード API の入口。集約ロジックは持たず [DashboardService] に任せる。
 *
 * 戻り値は同期的に返す（Callable / DeferredResult で非同期化していない）。
 * そのため下流 3 種類の呼び出しが終わるまで、このリクエストのサーブレットスレッドは占有されたままになる。
 * 並列版も同じく同期 API だが、内部で下流を並べて待つ点が違う。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
) {
    /**
     * 指定ユーザーのダッシュボードを返す。
     * 下流の失敗は例外のまま上がり、[com.example.bff.restclient.sequential.api.GlobalExceptionHandler] が HTTP ステータスに変換する。
     */
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): DashboardResponse =
        dashboardService.buildDashboard(userId)
}
