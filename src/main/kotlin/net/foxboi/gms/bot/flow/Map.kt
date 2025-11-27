package net.foxboi.gms.bot.flow


private abstract class MappingSubscriber<T, U>(val subscriber: Subscriber<U>) : Subscriber<T> {
    private var internalError = false

    override fun onSubscribe(subscription: Subscription) {
        subscriber.onSubscribe(subscription)
    }

    override fun onNext(element: T) {
        if (internalError) {
            return
        }

        val mapped = try {
            mapElement(element)
        } catch (e: Throwable) {
            internalError = true
            subscriber.onError(e)
            return
        }

        subscriber.onNext(mapped)
    }

    override fun onError(error: Throwable) {
        if (internalError) {
            return
        }

        subscriber.onError(error)
    }

    override fun onComplete() {
        if (internalError) {
            return
        }

        subscriber.onComplete()
    }

    protected abstract fun mapElement(element: T): U
}

private class MappedSubscriber<T, U>(
    subscriber: Subscriber<U>,
    val map: (T) -> U
) : MappingSubscriber<T, U>(subscriber) {
    override fun mapElement(element: T) = map(element)
}


private class MapMono<T, U>(val src: Mono<T>, val map: (T) -> U) : Mono<U>() {
    override fun subscribe(subscriber: Subscriber<U>) {
        src.subscribe(MappedSubscriber(subscriber, map))
    }
}

fun <T, U> Mono<T>.map(mapping: (T) -> U): Mono<U> {
    return MapMono(this, mapping)
}