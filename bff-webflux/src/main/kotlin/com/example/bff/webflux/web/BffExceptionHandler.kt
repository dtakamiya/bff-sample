package com.example.bff.webflux.web

import com.example.bff.webflux.model.ErrorBody
import com.example.bff.webflux.service.DownstreamFailureException
import com.example.bff.webflux.service.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 例外を HTTP ステータスに変換する。方針は次の 2 つ。
 *
 * - 利用者が存在しない（UserNotFoundException）だけを 404 にする。
 * - それ以外の下流の失敗は、下流の 404 や 500、タイムアウトを含めてすべて 502 にする。
 *   BFF から見れば依存先が正常に応答しなかったのであり、要求された利用者が無いわけではないため。
 *
 * Mono の onError として届いた例外がここで処理される。ハンドラの戻り値は ResponseEntity を使い、
 * ステータスと本文を明示する。
 */
@RestControllerAdvice
class BffExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody(exception.message.orEmpty()))

    @ExceptionHandler(DownstreamFailureException::class)
    fun handleDownstreamFailure(exception: DownstreamFailureException): ResponseEntity<ErrorBody> {
        // 下流の詳細（原因の型やスタックトレース）はログにだけ出す。
        // クライアントには汎用メッセージを返し、下流のホスト名や内部のエラー内容を漏らさない。
        log.warn("Downstream call failed: {}", exception.cause?.toString(), exception)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorBody(DOWNSTREAM_ERROR_MESSAGE))
    }

    private companion object {
        const val DOWNSTREAM_ERROR_MESSAGE = "downstream service unavailable"
    }
}
