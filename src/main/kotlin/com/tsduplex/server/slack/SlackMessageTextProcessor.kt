package com.tsduplex.server.slack

import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.User
import java.util.regex.Pattern


class SlackMessageTextProcessor(
        private val event: Event,
        private val users: MutableList<User>
) {
    val userIdRegex = Pattern.compile("<@([WU][^>]*)>")
    private val from: String = users.first { it.id == event.userId }.profile.realName

    fun fileCaption(): String {
        val sentBy = "Sent by: $from"
        val commentWrapper = event.file.initialComment ?: return sentBy
        return "${commentWrapper.comment}\n$sentBy"
                .escape()
                .replaceUserIdWithName()
    }

    fun imageCaption(): String {
        val sentBy = "Sent by: $from"
        val fileName = event.file.name
        val commentWrapper = event.file.initialComment
                ?: return "${event.file.name}\n$sentBy"

        return "${commentWrapper.comment}\n$fileName\n$sentBy"
                .escape()
                .replaceUserIdWithName()
    }

    fun snippet(downloadedContent: String): String {
        return "$from:\n$downloadedContent"
    }

    fun regularText(): String {
        return "$from:\n${event.text}"
                .escape()
                .replaceUserIdWithName()
    }

    // https://api.slack.com/docs/message-formatting#how_to_escape_characters
    private fun String.escape(): String {
        return this // not the best implementation though
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
    }

    // <@UW123> to @UserName
    private fun String.replaceUserIdWithName(): String {
        val matcher = userIdRegex.matcher(this)
        val userIds = ArrayList<String>()
        while (matcher.find()) {
            userIds.add(matcher.group(1))
        }

        var resulted = this
        userIds
                .forEach { userId ->
                    val realName = users.first { user -> user.id == userId }.profile.realName
                    resulted = resulted.replace("<@$userId>", "@$realName")
                }
        return resulted
    }
}