package net.foxboi.gms.sticker

import net.foxboi.gms.bot.Sticker

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