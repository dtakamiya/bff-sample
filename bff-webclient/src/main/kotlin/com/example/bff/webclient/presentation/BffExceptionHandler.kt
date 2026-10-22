package com.example.bff.webclient.presentation

import com.example.bff.webclient.domain.exception.DownstreamFailureException
import com.example.bff.webclient.domain.exception.UserNotFoundException
import com.example.bff.webclient.presentation.dto.ErrorBody
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * ドメイン例外を HTTP ステータスに変換する共通ハンドラ（presentation 層。全コントローラに適用）。
 *
 * マッピング方針:
 *  - UserNotFoundException → 404（利用者が指定したユーザーが存在しない）
 *  - DownstreamFailureException → 502（この BFF の責任ではない下流の失敗）
 *  - それ以外（IllegalStateException など）はここでは扱わず、Spring の既定の 500 になる。
 */
@RestControllerAdvice
class BffExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 404 の場合は例外メッセージ（"user not found: <id>"）をそのまま返す。 */
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody(exception.message.orEmpty()))

    /**
     * 502 の場合、利用者へは固定文言だけを返す（下流のホスト名や例外の詳細を外に出さないため）。
     * 原因はサーバ側の WARN ログに残す。障害調査では、このログの cause を見る。
     */
    @ExceptionHandler(DownstreamFailureException::class)
    fun handleDownstreamFailure(exception: DownstreamFailureException): ResponseEntity<ErrorBody> {
        // 末尾に exception を渡すと、SLF4J がスタックトレースもログに出す。
        log.warn("Downstream call failed: {}", exception.cause?.toString(), exception)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorBody(DOWNSTREAM_ERROR_MESSAGE))
    }

    private companion object {
        const val DOWNSTREAM_ERROR_MESSAGE = "downstream service unavailable"
    }
}
