package com.tsduplex.server.slack

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsduplex.server.shared.SlackBotProps
import me.ramswaroop.jbot.core.slack.models.Attachment
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject


@Component
class SlackWebApi(
        val slackBotProps: SlackBotProps,
        val objectMapper: ObjectMapper
) {
    val restTemplate = RestTemplate()

    val defaultHeaders = HttpHeaders()
            .apply { set("Content-Type", "application/json; charset=utf-8") }

    fun downloadFileAsText(uri: String): String {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${slackBotProps.slackBotToken}")
        }

        return restTemplate
                .exchange<String>(uri, HttpMethod.GET, HttpEntity<Void>(headers), String::class.java)
                .body!!
    }

    fun downloadFileAsByteArray(uri: String): ByteArray {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${slackBotProps.slackBotToken}")
        }

        val res = restTemplate
                .exchange(uri, HttpMethod.GET, HttpEntity<String>(headers), ByteArray::class.java)

        if (res.statusCode !== HttpStatus.OK)
            throw RuntimeException("File isn't available")
        else
            return res.body!!
    }

    fun uploadFile(data: ByteArray, filename: String, sentBy: String, channel: String) {
        val urlParams = mapOf(
                "token" to slackBotProps.slackBotToken,
                "channel" to channel,
                "initial_comment" to sentBy
        )

        val urlTemplate = "${slackBotProps.slackWebUri}/files.upload?token={token}&channels={channel}&initial_comment={initial_comment}"

        val multipartData = LinkedMultiValueMap<String, Any>()
        multipartData.add("file", object : ByteArrayResource(data) {
            override fun getFilename(): String? {
                return filename
            }
        })

        restTemplate
                .postForObject(urlTemplate, multipartData, String::class.java, urlParams)
    }

    fun postMessage(text: String, channel: String) {
        val urlParams = mapOf(
                "token" to slackBotProps.slackBotToken,
                "channel" to channel,
                "text" to text
        )

        val urlTemplate = "${slackBotProps.slackWebUri}/chat.postMessage?token={token}&channel={channel}&text={text}"

        restTemplate
                .postForObject<Any>(
                        urlTemplate,
                        HttpEntity<Any>(defaultHeaders),
                        urlParams
                )
    }

    fun postMessageWithTextForwarded(attachment: String, channel: String) {
        val attachments = listOf(Attachment().apply { setText(attachment) })
        val urlParams = mutableMapOf(
                "token" to slackBotProps.slackBotToken,
                "channel" to channel,
                "attachments" to objectMapper.writeValueAsString(attachments)
        )
        val urlTemplate = "${slackBotProps.slackWebUri}" +
                "/chat.postMessage?token={token}&channel={channel}&attachments={attachments}"

        postRichTextMessageInternal(urlTemplate, urlParams)
    }

    fun postMessageWithTextReply(text: String, attachment: String, channel: String) {
        val attachments = listOf(Attachment().apply { setText(attachment) })
        val urlParams = mutableMapOf(
                "token" to slackBotProps.slackBotToken,
                "channel" to channel,
                "attachments" to objectMapper.writeValueAsString(attachments),
                "text" to text
        )
        val urlTemplate = "${slackBotProps.slackWebUri}" +
                "/chat.postMessage?token={token}&channel={channel}&text={text}&attachments={attachments}"

        postRichTextMessageInternal(urlTemplate, urlParams)
    }

    private fun postRichTextMessageInternal(urlTemplate: String, urlParams : Map<String, String>) {
        restTemplate
                .postForObject(
                        urlTemplate,
                        HttpEntity<Any>(defaultHeaders),
                        String::class.java,
                        urlParams
                )
    }
}