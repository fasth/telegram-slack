package com.tsduplex.server.slack

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.request.SendDocument
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPhoto
import com.pengrad.telegrambot.response.SendResponse
import java.io.IOException

class TextSentCallback : Callback<SendMessage, SendResponse> {
    override fun onFailure(request: SendMessage, e: IOException) {
    }

    override fun onResponse(request: SendMessage, response: SendResponse) {
    }
}

class DocumentSentCallback : Callback<SendDocument, SendResponse> {
    override fun onFailure(request: SendDocument, e: IOException) {
    }

    override fun onResponse(request: SendDocument, response: SendResponse) {
    }
}

class PhotoSentCallback : Callback<SendPhoto, SendResponse> {
    override fun onFailure(request: SendPhoto, e: IOException) {
    }

    override fun onResponse(request: SendPhoto, response: SendResponse) {
    }
}