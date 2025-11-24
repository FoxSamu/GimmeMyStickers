package net.foxboi.stickerbot.bot.flow

interface Subscriber<in T> { // A subscriber to Object can subscribe to a publisher of String
    fun onSubscribe(subscription: Subscription)

    fun onNext(element: T)

    fun onError(error: Throwable)

    fun onComplete()
}