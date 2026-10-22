package com.example.bff.restclient.sequential.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class DownstreamConfig {

    @Bean
    fun downstreamRestClient(
        builder: RestClient.Builder,
        properties: DownstreamProperties,
    ): RestClient {
        val timeout = Duration.ofMillis(properties.timeoutMs)
        val requestFactory = JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(timeout).build(),
        ).apply { setReadTimeout(timeout) }
        return builder
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
