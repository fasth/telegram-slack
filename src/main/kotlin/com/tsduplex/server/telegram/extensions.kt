package com.tsduplex.server.telegram

import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.MessageEntity

fun Message.computeType(): TelegramMessageType {
    this.forwardFrom()?.let { return TelegramMessageType.TEXT_FORWARDED_USER }
    this.forwardFromChat()?.let { return TelegramMessageType.TEXT_FORWARDED_CHAT }
    this.replyToMessage()?.let {
        return TelegramMessageType.TEXT_WITH_REPLY
    }
    if (this.caption() != null) {
        this.photo()?.let { return TelegramMessageType.PHOTO_WITH_CAPTION }
        this.document()?.let { return TelegramMessageType.FILE_WITH_CAPTION }
        this.video()?.let { return TelegramMessageType.VIDEO_WITH_CAPTION }
    }
    this.photo()?.let { return TelegramMessageType.PHOTO }
    this.document()?.let { return TelegramMessageType.FILE }
    this.video()?.let { return TelegramMessageType.VIDEO }
    this.text()?.also { if (entities() == null) return TelegramMessageType.TEXT }
    this.entities()
            ?.also {
                return if (this.entities().any { it.isKeyCommand(this.text()) })
                    TelegramMessageType.KEY_CONFIRMATION
                else
                    TelegramMessageType.TEXT
            }

    return TelegramMessageType.UNKNOWN
}

fun MessageEntity.isKeyCommand(text: String): Boolean {
    return type() == MessageEntity.Type.bot_command
            && text.take(length()) == "/key"
}