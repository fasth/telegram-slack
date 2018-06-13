package com.tsduplex.server.telegram

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class TelegramWebApi {

    val restTemplate = RestTemplate()

    fun downloadFile(uri: String): ByteArray {
        val headers = HttpHeaders()
                .apply { accept = arrayListOf(MediaType.APPLICATION_OCTET_STREAM) }

        val res = restTemplate
                .exchange(uri, HttpMethod.GET, HttpEntity<String>(headers), ByteArray::class.java)

        if (res.statusCode !== HttpStatus.OK)
            throw RuntimeException("File isn't available")
        else
            return res.body!!
    }
}