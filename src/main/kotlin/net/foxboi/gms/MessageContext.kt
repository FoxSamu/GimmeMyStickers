package net.foxboi.gms

import net.foxboi.gms.bot.Bot
import net.foxboi.gms.bot.Chat
import net.foxboi.gms.bot.Message

class MessageContext(
    val bot: Bot,
    val session: Session,
    val chat: Chat,
    val message: Message
)