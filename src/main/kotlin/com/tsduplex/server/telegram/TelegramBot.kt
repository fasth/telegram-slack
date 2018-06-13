package com.tsduplex.server.telegram

import com.pengrad.telegrambot.TelegramBot
import com.tsduplex.server.shared.TelegramBotProps
import org.springframework.stereotype.Component

@Component
class TelegramWebhookBot(val telegramBotProps: TelegramBotProps)
    : TelegramBot(telegramBotProps.token)