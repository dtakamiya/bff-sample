package com.example.bff.webclient

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BffWebclientApplication

fun main(args: Array<String>) {
    runApplication<BffWebclientApplication>(*args)
}
