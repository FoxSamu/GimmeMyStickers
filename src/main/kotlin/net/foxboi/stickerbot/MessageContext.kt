package net.foxboi.stickerbot

import net.foxboi.stickerbot.bot.Bot
import net.foxboi.stickerbot.bot.Chat
import net.foxboi.stickerbot.bot.Message

class MessageContext(
    val bot: Bot,
    val session: Session,
    val chat: Chat,
    val message: Message
)