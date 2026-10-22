package com.example.bff.webflux.web

import com.example.bff.webflux.model.ErrorBody
import com.example.bff.webflux.service.DownstreamFailureException
import com.example.bff.webflux.service.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BffExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(exception: UserNotFoundException): ResponseEntity<ErrorBody> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorBody(exception.message.orEmpty()))

    @ExceptionHandler(DownstreamFailureException::class)
    fun handleDownstreamFailure(exception: DownstreamFailureException): ResponseEntity<ErrorBody> {
        log.warn("Downstream call failed: {}", exception.cause?.toString(), exception)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorBody(DOWNSTREAM_ERROR_MESSAGE))
    }

    private companion object {
        const val DOWNSTREAM_ERROR_MESSAGE = "downstream service unavailable"
    }
}
