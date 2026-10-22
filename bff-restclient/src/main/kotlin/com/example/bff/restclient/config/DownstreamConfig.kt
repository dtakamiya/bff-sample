package com.example.bff.restclient.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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

    @Bean(destroyMethod = "shutdown")
    fun downstreamExecutor(properties: DownstreamProperties): ExecutorService =
        Executors.newFixedThreadPool(properties.parallelism)
}
