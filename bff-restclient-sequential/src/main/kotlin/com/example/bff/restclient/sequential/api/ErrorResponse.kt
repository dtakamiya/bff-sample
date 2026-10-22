package com.example.bff.restclient.sequential.api

/**
 * エラー時の共通レスポンスボディ。
 *
 * [error] にはクライアント向けの短い文言だけを入れる。
 * 下流のホスト名・URL・スタックトレースは入れない（詳細はサーバーログに残す）。
 */
data class ErrorResponse(
    val error: String,
)
