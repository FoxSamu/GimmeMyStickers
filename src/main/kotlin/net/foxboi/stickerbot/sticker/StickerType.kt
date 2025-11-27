package net.foxboi.stickerbot.sticker

import net.foxboi.stickerbot.bot.Sticker

enum class StickerFormat {
    WEBP,
    TGS,
    WEBM
}

val Sticker.format
    get() = when {
        isVideo -> StickerFormat.WEBM
        isAnimated -> StickerFormat.TGS
        else -> StickerFormat.WEBP
    }