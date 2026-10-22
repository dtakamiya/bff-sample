package com.example.bff.restclient

import com.example.bff.restclient.config.DownstreamProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DownstreamProperties::class)
class BffRestclientApplication

fun main(args: Array<String>) {
    runApplication<BffRestclientApplication>(*args)
}
