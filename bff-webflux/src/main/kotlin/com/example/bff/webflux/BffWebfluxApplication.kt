package com.example.bff.webflux

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class BffWebfluxApplication

fun main(args: Array<String>) {
    runApplication<BffWebfluxApplication>(*args)
}
