package com.example.bff.webclient.service

class UserNotFoundException(userId: String) : RuntimeException("user not found: $userId")

class DownstreamFailureException(cause: Throwable) : RuntimeException("downstream call failed", cause)
