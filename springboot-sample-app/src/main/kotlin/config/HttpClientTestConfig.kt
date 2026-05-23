package com.kraftadmin.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class HttpClientTestConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}