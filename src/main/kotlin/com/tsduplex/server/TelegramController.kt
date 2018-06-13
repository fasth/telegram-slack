package com.tsduplex.server

import com.pengrad.telegrambot.BotUtils
import com.tsduplex.server.shared.TelegramBotProps
import com.tsduplex.server.telegram.TelegramHandler
import mu.KLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class TelegramController(
        val telegramBotProps: TelegramBotProps,
        val telegramHandler: TelegramHandler
)  {
    companion object : KLogging()

    @PostMapping(value = "b8e18df8-6698-11e8-adc0-fa7ae01bbebc/bot{token}")
    @ResponseStatus(code = HttpStatus.OK)
    fun handleUpdate(@PathVariable token: String, @RequestBody updateRaw: String) {
        if (token != telegramBotProps.token)
            throw InvalidTokenException()

        val message = BotUtils.parseUpdate(updateRaw).message() ?: return
        telegramHandler.handleUpdateMessage(message)
    }

    @PostMapping(value = "hello")
    @ResponseStatus(code = HttpStatus.OK)
    fun hello(): String {
        return "Hello"
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    inner class InvalidTokenException : RuntimeException()
}

