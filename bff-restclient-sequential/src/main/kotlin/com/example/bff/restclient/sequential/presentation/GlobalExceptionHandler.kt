package com.example.bff.restclient.sequential.presentation

import com.example.bff.restclient.sequential.domain.exception.DownstreamException
import com.example.bff.restclient.sequential.domain.exception.UserNotFoundException
import com.example.bff.restclient.sequential.presentation.dto.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** 404 時の error 文言の先頭部分。 `"$USER_NOT_FOUND_MESSAGE: <userId>"` の形で返す。 */
const val USER_NOT_FOUND_MESSAGE = "user not found"

/** 502 時の固定文言。下流の障害理由は外に出さない。 */
const val DOWNSTREAM_FAILURE_MESSAGE = "downstream service unavailable"

/**
 * ドメイン例外を HTTP ステータスに対応づける（presentation 層）。エラーマッピングの方針は次の 2 つだけ。
 *
 * - user が存在しない（下流 user が 404、または空白のみのパス ID）→ **404**。
 * - それ以外の下流失敗 → **502 Bad Gateway**。
 *   接続失敗・タイムアウト・5xx・想定外の 4xx・空ボディ・変換失敗に加え、**注文の商品が 404 だった場合も 502** になる。
 *   商品不在をクライアントの 404 にはしない（user の 404 と区別できないと、画面側で誤解するため）。
 *
 * ログの重さも分けている。404 は利用者側の入力ミスなので info、502 は下流の障害なので
 * スタックトレース付きの warn にして運用側が気づけるようにする。
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        log.info("user not found: {}", e.userId)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("$USER_NOT_FOUND_MESSAGE: ${e.userId}"))
    }

    /**
     * 下流の障害を 502 にする。原因（どの呼び出しが、なぜ失敗したか）は [DownstreamException.message] と
     * cause に入っているので、クライアントには返さずログにだけ出す。
     */
    @ExceptionHandler(DownstreamException::class)
    fun handleDownstreamFailure(e: DownstreamException): ResponseEntity<ErrorResponse> {
        log.warn("downstream failure: {}", e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(DOWNSTREAM_FAILURE_MESSAGE))
    }
}
