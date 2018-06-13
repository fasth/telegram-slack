package com.tsduplex.server.slack

import me.ramswaroop.jbot.core.slack.models.Event
import org.springframework.http.MediaType

internal enum class SlackMessageType {
    TEXT, SNIPPET, PHOTO, FILE, UNKNOWN
}

internal fun isImage(mimeType: String): Boolean {
    return listOf(MediaType.IMAGE_GIF_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE)
            .any { mimeType == it }
}

internal fun isText(mimeType: String): Boolean {
    return mimeType == MediaType.TEXT_PLAIN_VALUE
}

internal fun Event.computeType(): SlackMessageType {
    if (this.type == "message" && this.subtype == null)
        return SlackMessageType.TEXT

    if (this.subtype == "file_share") {
        if (isImage(this.file.mimetype)) return SlackMessageType.PHOTO
        if (isText(this.file.mimetype)) return SlackMessageType.SNIPPET
        return SlackMessageType.FILE
    }

    return SlackMessageType.UNKNOWN
}