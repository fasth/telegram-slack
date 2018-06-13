package com.tsduplex.server.shared

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit


@Configuration
class Configuration {

    @Bean
    fun objectMapper() = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    }

    @Bean
    fun yamlObjectMapper() = ObjectMapper(YAMLFactory())
            .apply { registerModule(KotlinModule()) }

    @Bean
    fun httpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .readTimeout(10000, TimeUnit.MILLISECONDS) // constant should be moved to props
                .writeTimeout(10000, TimeUnit.MILLISECONDS)
                .connectTimeout(10000, TimeUnit.MILLISECONDS)
                .build()
    }
}