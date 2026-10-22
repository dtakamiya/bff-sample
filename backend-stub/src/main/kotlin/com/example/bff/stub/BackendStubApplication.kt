package com.example.bff.stub

import com.example.bff.stub.config.StubProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(StubProperties::class)
class BackendStubApplication

fun main(args: Array<String>) {
    runApplication<BackendStubApplication>(*args)
}
