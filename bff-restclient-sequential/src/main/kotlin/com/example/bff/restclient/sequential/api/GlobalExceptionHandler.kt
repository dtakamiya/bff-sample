package com.example.bff.restclient.sequential.api

import com.example.bff.restclient.sequential.client.DownstreamException
import com.example.bff.restclient.sequential.client.UserNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

const val USER_NOT_FOUND_MESSAGE = "user not found"
const val DOWNSTREAM_FAILURE_MESSAGE = "downstream service unavailable"

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(e: UserNotFoundException): ResponseEntity<ErrorResponse> {
        log.info("user not found: {}", e.userId)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("$USER_NOT_FOUND_MESSAGE: ${e.userId}"))
    }

    @ExceptionHandler(DownstreamException::class)
    fun handleDownstreamFailure(e: DownstreamException): ResponseEntity<ErrorResponse> {
        log.warn("downstream failure: {}", e.message, e)
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ErrorResponse(DOWNSTREAM_FAILURE_MESSAGE))
    }
}
