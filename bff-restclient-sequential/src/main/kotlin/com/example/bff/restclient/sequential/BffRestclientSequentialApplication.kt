package com.example.bff.restclient.sequential

import com.example.bff.restclient.sequential.config.DownstreamProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DownstreamProperties::class)
class BffRestclientSequentialApplication

fun main(args: Array<String>) {
    runApplication<BffRestclientSequentialApplication>(*args)
}
