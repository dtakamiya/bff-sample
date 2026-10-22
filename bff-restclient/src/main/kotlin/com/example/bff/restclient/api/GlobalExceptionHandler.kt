package com.example.bff.restclient.api

import com.example.bff.restclient.client.DownstreamException
import com.example.bff.restclient.client.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// 応答に出す固定文言。下流の詳細（URL・例外メッセージ）を外に漏らさないため、例外の message は使わない。
const val USER_NOT_FOUND_MESSAGE = "user not found"
const val DOWNSTREAM_FAILURE_MESSAGE = "downstream service unavailable"

/**
 * 例外を HTTP 応答へ変換する集約点。コントローラごとに try/catch を散らさないためにここへ集めている。
 *
 * 変換ルール:
 * - [UserNotFoundException] → 404。原因は利用者の入力（存在しない ID）なので、ID を含めた文言を返す。
 * - [DownstreamException] → 502（Bad Gateway）。BFF 自身は動いているが、下流が応答できないという意味。
 *   応答は固定文言のみにし、詳細は WARN ログに残す。
 *
 * 注意: 注文・商品側の 404 も [DownstreamException] として扱われるため 502 になる。
 * 404 になるのはユーザーが存在しない場合だけ。
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        // 404 は想定内の業務エラーなので INFO。スタックトレースは残さない。
        log.info("user not found: {}", e.userId)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("$USER_NOT_FOUND_MESSAGE: ${e.userId}"))
    }

    @ExceptionHandler(DownstreamException::class)
    fun handleDownstreamFailure(e: DownstreamException): ResponseEntity<ErrorResponse> {
        // 下流障害は運用で気づく必要があるため WARN。末尾に例外を渡してスタックトレースを残す（SLF4J の慣例）。
        log.warn("downstream failure: {}", e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(DOWNSTREAM_FAILURE_MESSAGE))
    }
}
