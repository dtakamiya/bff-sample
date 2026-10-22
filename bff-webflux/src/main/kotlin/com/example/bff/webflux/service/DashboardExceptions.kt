package com.example.bff.webflux.service

class UserNotFoundException(userId: String) : RuntimeException("user not found: $userId")

class DownstreamFailureException(cause: Throwable) : RuntimeException("downstream call failed", cause)
