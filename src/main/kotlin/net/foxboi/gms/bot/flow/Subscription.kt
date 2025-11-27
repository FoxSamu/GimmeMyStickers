package net.foxboi.gms.bot.flow

interface Subscription {
    fun request(amount: Long)

    fun cancel()
}