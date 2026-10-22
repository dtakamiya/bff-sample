package com.example.bff.restclient.presentation

import com.example.bff.restclient.application.usecase.GetDashboardUseCase
import com.example.bff.restclient.presentation.dto.DashboardResponse
import com.example.bff.restclient.presentation.dto.toResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ダッシュボード取得の HTTP 入口（presentation 層）。
 *
 * ここは委譲だけに徹する。パスを受けてユースケースを呼び、結果を画面向けの形へ写すだけ。
 * 下流の並列呼び出しと集約は [GetDashboardUseCase]、例外の HTTP ステータス変換は [GlobalExceptionHandler] が担うため、
 * このクラスに try/catch は置かない。
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val getDashboardUseCase: GetDashboardUseCase,
) {
    /**
     * 指定ユーザーのダッシュボードを返す。
     * 下流の失敗は例外のまま上がり、[GlobalExceptionHandler] が 404（ユーザー不在）や 502（下流障害）に変換する。
     */
    @GetMapping("/{userId}")
    fun getDashboard(@PathVariable("userId") userId: String): DashboardResponse =
        getDashboardUseCase.execute(userId).toResponse()
}
