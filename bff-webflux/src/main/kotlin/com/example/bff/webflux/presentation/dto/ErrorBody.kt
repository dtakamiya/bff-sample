package com.example.bff.webflux.presentation.dto

/** エラー時のレスポンスボディ（presentation 層）。BffExceptionHandler が `{"error": "..."}` の形で返す。 */
data class ErrorBody(
    val error: String,
)
