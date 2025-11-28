/*
 * Copyright (c) 2025 Olaf W. Nankman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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