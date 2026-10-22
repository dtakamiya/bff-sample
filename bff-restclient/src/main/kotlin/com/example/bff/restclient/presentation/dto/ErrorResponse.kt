package com.example.bff.restclient.presentation.dto

/**
 * エラー時の応答ボディ（例: `{"error": "user not found: u9"}`）。presentation 層。
 *
 * 成功時の応答はこの型で包まず、[DashboardResponse] をそのまま返す。
 * [error] には利用者に見せてよい文言だけを入れ、下流の URL や例外の詳細は含めない。
 */
data class ErrorResponse(
    val error: String,
)
