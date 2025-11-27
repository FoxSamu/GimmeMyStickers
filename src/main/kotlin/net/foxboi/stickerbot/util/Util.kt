package net.foxboi.stickerbot.util

import io.ktor.http.*

val ContentType.Application.TgSticker by lazy { ContentType("application", "x-tgsticker") }
