package com.tsduplex.server.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.GetFile
import com.tsduplex.server.shared.ConfluenceRepository
import com.tsduplex.server.slack.SlackWebApi
import com.tsduplex.server.telegram.TelegramMessageType.FILE
import com.tsduplex.server.telegram.TelegramMessageType.FILE_WITH_CAPTION
import com.tsduplex.server.telegram.TelegramMessageType.KEY_CONFIRMATION
import com.tsduplex.server.telegram.TelegramMessageType.PHOTO
import com.tsduplex.server.telegram.TelegramMessageType.PHOTO_WITH_CAPTION
import com.tsduplex.server.telegram.TelegramMessageType.TEXT
import com.tsduplex.server.telegram.TelegramMessageType.TEXT_FORWARDED_CHAT
import com.tsduplex.server.telegram.TelegramMessageType.TEXT_FORWARDED_USER
import com.tsduplex.server.telegram.TelegramMessageType.TEXT_WITH_REPLY
import com.tsduplex.server.telegram.TelegramMessageType.VIDEO
import com.tsduplex.server.telegram.TelegramMessageType.VIDEO_WITH_CAPTION
import mu.KLogging
import org.springframework.stereotype.Component

enum class TelegramMessageType {
    KEY_CONFIRMATION,
    TEXT, TEXT_WITH_REPLY, TEXT_FORWARDED_CHAT, TEXT_FORWARDED_USER,
    PHOTO, PHOTO_WITH_CAPTION,
    FILE, FILE_WITH_CAPTION,
    VIDEO, VIDEO_WITH_CAPTION,
    UNKNOWN
}

@Component
class TelegramHandler(
        val confluenceRepository: ConfluenceRepository,
        val messageTextProcessor: TelegramMessageTextProcessor,
        val slackWebApi: SlackWebApi,
        val telegramWebApi: TelegramWebApi,
        val telegramBot: TelegramBot
) {
    companion object : KLogging()

    val retrieveImageFileId = { m: Message -> m.photo().last().fileId() } // best quality
    val retrieveDocumentFileId = { m: Message -> m.document().fileId() }
    val retrieveVideoFileId = { m: Message -> m.video().fileId() }

    val retrieveImageName = { message: Message ->
        message
                .document()?.fileName()
                ?: messageTextProcessor.generateImageName("photo")
    }
    val retrieveDocumentName = { m: Message -> m.document().fileName() }
    val retrieveVideoName = { m: Message ->
        m.document()?.fileName() ?: messageTextProcessor.generateImageName("video")
    }

    fun handleUpdateMessage(message: Message) {
        logger.info { message }
        when (message.computeType()) {
            KEY_CONFIRMATION -> handleKeyCommand(message)
            TEXT -> handleText(message)
            TEXT_WITH_REPLY -> handleReply(message)
            TEXT_FORWARDED_USER -> handleForwarded(message, TEXT_FORWARDED_USER)
            TEXT_FORWARDED_CHAT -> handleForwarded(message, TEXT_FORWARDED_CHAT)
            PHOTO, PHOTO_WITH_CAPTION -> handleBinary(message, retrieveImageFileId, retrieveImageName)
            FILE, FILE_WITH_CAPTION -> handleBinary(message, retrieveDocumentFileId, retrieveDocumentName)
            VIDEO, VIDEO_WITH_CAPTION -> handleBinary(message, retrieveVideoFileId, retrieveVideoName)
            else -> return
        }
    }

    private fun getSlackChannel(message: Message): String? {
        return confluenceRepository
                .confluences
                .filter { !it.isWaitingForConfirmation() }
                .singleOrNull { it.telegramGroupId == message.chat().id().toInt() }
                ?.slackChannelId
    }

    private fun handleBinary(
            message: Message,
            fileIdRetriever: (Message) -> String,
            fileNameRetriever: (Message) -> String
    ) {
        logger.info("handle file...   $fileNameRetriever")
        val channel = getSlackChannel(message) ?: return
        val getFileRequest = GetFile(fileIdRetriever(message))
        val file = telegramBot
                .execute(getFileRequest)
                .file()

        val fileData = telegramWebApi
                .downloadFile(telegramBot.getFullFilePath(file))

        val fullName = messageTextProcessor.processFullName(message)
        val caption = if (message.caption() == null)
            "Sent by: $fullName"
        else
            "${message.caption()}\nSent by: $fullName"

        slackWebApi
                .uploadFile(fileData, fileNameRetriever(message), caption, channel)
    }

    private fun handleText(message: Message) {
        val slackChannel = getSlackChannel(message) ?: return
        val fullName = messageTextProcessor.processFullName(message)
        val formattedText = messageTextProcessor.processText(message)

        val processedText = """
            |$fullName:
            |$formattedText
            |"""
                .trimMargin()

        slackWebApi.postMessage(processedText, slackChannel)
    }

    private fun handleReply(message: Message) {
        val slackChannel = getSlackChannel(message) ?: return
        val fullName = messageTextProcessor.processFullName(message)
        val formattedText = messageTextProcessor.processText(message)

        val processedText = """
            |$fullName:
            |$formattedText
            |"""
                .trimMargin()

        val replied = message.replyToMessage().text()
        val fullNameReplied = messageTextProcessor.processFullName(message.replyToMessage())
        val processedReplied = """
            |$fullNameReplied:
            |$replied
            |"""
                .trimMargin()

        slackWebApi.postMessageWithTextReply(processedText, processedReplied, slackChannel)
    }

    private fun handleForwarded(message: Message, forwardType: TelegramMessageType) {
        val slackChannel = getSlackChannel(message) ?: return

        val forwardedFrom =
                if (forwardType == TEXT_FORWARDED_USER)
                    messageTextProcessor.processForwardedUser(message.forwardFrom())
                else
                    messageTextProcessor.processForwardedChat(message.forwardFromChat())

        val formattedText = messageTextProcessor.processText(message)

        val processedText = """
            |$forwardedFrom:
            |$formattedText
            |"""
                .trimMargin()

        slackWebApi.postMessageWithTextForwarded(processedText, slackChannel)
    }

    private fun handleKeyCommand(message: Message) {
        val keyToBeConfirmed = message
                .text()
                .substring(message.entities().first().length())
                .trim()

        val confluence = confluenceRepository
                .confluences
                .filter { it.isWaitingForConfirmation() }
                .singleOrNull { it.confluenceKey == keyToBeConfirmed } ?: return

        confluence.apply {
            telegramGroupId = message.chat().id().toInt()
            telegramGroupName = message.chat().title()
        }

        confluenceRepository.update(confluence)

        slackWebApi.postMessage("Group connected: ${message.chat().title()}", confluence.slackChannelId)
    }
}
