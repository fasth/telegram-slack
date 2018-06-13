package com.tsduplex.server.telegram

import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.User
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class TelegramMessageTextProcessor {

    fun processFullName(message: Message): String {
        val firstName = message.from().firstName()
        val lastName = message.from().lastName() // nullable

        return lastName
                ?.let { ln -> "$firstName $ln" }
                ?: firstName
    }

    fun processForwardedChat(chat: Chat): String {
        return "Forwarded from ${chat.title()}"
    }

    fun processForwardedUser(user: User): String {
        val firstName = user.firstName()
        val lastName = user.lastName() // nullable

        return lastName
                ?.let { ln -> "Forwarded from $firstName $ln" }
                ?: "Forwarded from $firstName"
    }

    fun processText(message: Message): String {
        return message.text() // stub
    }

    fun generateImageName(messageType: String): String {
        val date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH-mm-ss_yyyy-MM-dd"))

        return String.format("%s_%s", messageType, date)
    }
}