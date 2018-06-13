package com.tsduplex.server.slack

import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import com.tsduplex.server.shared.Confluence
import com.tsduplex.server.shared.ConfluenceRepository
import com.tsduplex.server.telegram.TelegramWebhookBot
import me.ramswaroop.jbot.core.common.Controller
import me.ramswaroop.jbot.core.common.EventType
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import mu.KLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.PingMessage
import org.springframework.web.socket.WebSocketSession
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


@Component
class SlackBot(
        val confluenceRepository: ConfluenceRepository,
        val telegramBot: TelegramWebhookBot,
        val slackWebApi: SlackWebApi
) : Bot() {

    companion object : KLogging()

    val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Value("\${slackBotToken}")
    private lateinit var slackToken: String

    override fun getSlackBot(): Bot {
        return this
    }

    override fun getSlackToken(): String {
        return slackToken
    }

    @Controller(events = [EventType.DIRECT_MESSAGE], pattern = "keys")
    fun onDM(session: WebSocketSession, event: Event) {
        val messageFromUser = slackService
                .users
                .first { it.id == event.userId }

        if (!messageFromUser.isAdmin) {
            reply(session, event, "Forbidden")
            return
        }

        confluenceRepository
                .confluences
                .joinToString(separator = "\n – ", prefix = "\n – ", postfix = "\n")
                .also { confluences ->
                    reply(session, event, confluences)
                }
    }

    @Controller(events = [EventType.CHANNEL_JOINED, EventType.GROUP_JOINED])
    fun onInvited(session: WebSocketSession, event: Event) {
        val slackConfluence = Confluence(event.channel.id, event.channel.name, UUID.randomUUID().toString())
        confluenceRepository
                .removeDangling(slackConfluence) // clean unanswered keys
        confluenceRepository
                .create(slackConfluence)
                .also { confluence ->
                    val groupMessage = Message("Confluence key: ${confluence.confluenceKey}")
                            .also { it.channel = event.channel.id }
                    reply(session, event, groupMessage)
                }
    }

    fun getTelegramGroup(event: Event): Int? {
        return confluenceRepository
                .confluences
                .filter { !it.isWaitingForConfirmation() }
                .singleOrNull { it.slackChannelId == event.channelId }
                ?.let { it.telegramGroupId }
    }

    @Controller(events = [EventType.MESSAGE])
    fun onMessageReceive(session: WebSocketSession, event: Event) {
        if (event.subtype == "bot_message") return
        val telegramGroupId = getTelegramGroup(event) ?: return

        val users = slackService.users
        val textProcessor = SlackMessageTextProcessor(event, users)

        when (event.computeType()) {
            SlackMessageType.TEXT -> {
                telegramBot.execute(SendMessage(telegramGroupId, textProcessor.regularText()), TextSentCallback())
            }
            SlackMessageType.SNIPPET -> {
                val content = slackWebApi.downloadFileAsText(event.file.urlPrivateDownload)
                telegramBot.execute(SendMessage(telegramGroupId, textProcessor.snippet(content)), TextSentCallback())
            }
            SlackMessageType.FILE -> {
                val content = slackWebApi.downloadFileAsByteArray(event.file.urlPrivateDownload)
                val sendDocumentRequest = SendDocument(telegramGroupId, content)
                        .caption(textProcessor.fileCaption())
                        .fileName(event.file.name)
                telegramBot.execute(sendDocumentRequest, DocumentSentCallback())
            }
            SlackMessageType.PHOTO -> {
                val content = slackWebApi.downloadFileAsByteArray(event.file.urlPrivateDownload)
                val sendPhotoRequest = SendPhoto(telegramGroupId, content)
                        .caption(textProcessor.imageCaption())
                        .fileName(event.file.name)
                telegramBot.execute(sendPhotoRequest, PhotoSentCallback())
            }
            else -> return
        }

    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.info("WebSocket is now connected: {}", session)
        executorService.scheduleAtFixedRate(Runnable {
            try {
                logger.error("Pinging...")
                session.sendMessage(PingMessage())
            } catch (e: Exception) {
                logger.error("Failed to deliver ping", e)
            }
        }, 10L, 45L, TimeUnit.SECONDS)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.error("Slack WS session has been closed, code: ${status.code}") // not expected!!!
    }
}