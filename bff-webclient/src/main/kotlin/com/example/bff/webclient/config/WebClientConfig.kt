package com.example.bff.webclient.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun downstreamWebClient(
        builder: WebClient.Builder,
        properties: DownstreamProperties,
    ): WebClient = builder.baseUrl(properties.baseUrl).build()
}
