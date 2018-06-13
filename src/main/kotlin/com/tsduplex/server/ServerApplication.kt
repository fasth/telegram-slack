package com.tsduplex.server

import com.pengrad.telegrambot.request.SetWebhook
import com.tsduplex.server.shared.TelegramBotProps
import com.tsduplex.server.telegram.TelegramWebhookBot
import mu.KLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = [
    "com.tsduplex.server",
    "me.ramswaroop.jbot"
]
)
@EnableScheduling
class ServerApplication

@Component
class Runner(
        val telegramWebhookBot: TelegramWebhookBot,
        val telegramBotProps: TelegramBotProps
) : CommandLineRunner {
    companion object : KLogging()


    override fun run(vararg args: String) {
        logger.info { "going to set telegram webhook api endpoint to ${telegramBotProps.webhookBaseUri}" }
        val setWebhook = SetWebhook()
                .url(telegramBotProps.webhookBaseUri)
        telegramWebhookBot.execute(setWebhook)
    }
}

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}