package com.example.bff.restclient.sequential.client

class UserNotFoundException(
    val userId: String,
    cause: Throwable? = null,
) : RuntimeException("user not found: $userId", cause)

class DownstreamException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
