package net.foxboi.stickerbot.bot.flow

import io.ktor.http.*
import kotlinx.io.Sink

class Upload(val filename: String, val contentType: ContentType? = null, val uploader: (Sink) -> Unit)