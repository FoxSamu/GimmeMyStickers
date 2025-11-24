package net.foxboi.stickerbot.bot.flow

interface Subscription {
    fun request(amount: Long)

    fun cancel()
}