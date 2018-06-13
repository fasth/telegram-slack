package com.tsduplex.server.shared

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank


@Component
@ConfigurationProperties(prefix = "telegram-bot")
@Validated
class TelegramBotProps {
    @NotBlank
    var token: String = ""

    @NotBlank
    var telegramBaseUri: String = ""

    @NotBlank
    var webhookBaseUri: String = ""
}

@Component
@ConfigurationProperties()
@Validated
class SlackBotProps {
    @NotBlank
    var slackBotToken: String = ""

    var slackWebUri: String = ""
}

@Component
@ConfigurationProperties(prefix = "storage")
class StorageProps {
    var dir: String = ""
}