package net.foxboi.stickerbot.bot.flow

interface Publisher<out T> { // A publisher of String can be subscribed to by a subscriber of Object
    fun subscribe(subscriber: Subscriber<T>)
}