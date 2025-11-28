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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.*

/**
 * A [Publisher] of exactly one element.
 */
abstract class Mono<T> : Publisher<T> {
    /**
     * Subscribes to this [Mono] and calls the given callbacks accordingly.
     */
    open fun subscribe(
        onComplete: ((T) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        subscribe(ListeningMonoSubscriber(
            receive = onComplete,
            receiveError = onError,
        ))
    }

    /**
     * Subscribes to this [Mono] and suspends until it produces a value.
     *
     * @return The produced value
     * @throws Throwable When the [Mono] produces an exception
     */
    open suspend fun get(): T {
        return suspendCoroutine { cont ->
            subscribe(ContinuingMonoSubscriber(cont))
        }
    }

    companion object {
        /**
         * Returns a [Mono] that directly supplies the given value.
         */
        fun <T> direct(value: T): Mono<T> {
            return DirectMono(value)
        }

        /**
         * Returns a [Mono] that produces a value suspendingly.
         */
        fun <T> produce(scope: CoroutineScope, producer: suspend CoroutineScope.() -> T): Mono<T> {
            return ProduceMono(scope, producer)
        }

        /**
         * Returns a [Mono] that produces a value suspendingly.
         */
        fun <T> produce(context: CoroutineContext, producer: suspend CoroutineScope.() -> T): Mono<T> {
            return ProduceMono(CoroutineScope(context), producer)
        }

        /**
         * Returns a [Mono] that produces a value suspendingly in the current coroutine.
         */
        suspend fun <T> produce(producer: suspend CoroutineScope.() -> T): Mono<T> {
            return ProduceMono(CoroutineScope(currentCoroutineContext()), producer)
        }
    }
}

private class DirectMono<T>(val value: T) : Mono<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(SingleElementSubscription(subscriber, value))
    }
}

private class ProduceMono<T>(val scope: CoroutineScope, val producer: suspend CoroutineScope.() -> T) : Mono<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(CoroutineSingletonProducerSubscription(subscriber, scope, producer))
    }
}


// ==============================================================================================
// Default subscriber
// ==============================================================================================

private enum class MonoReceived {
    NOTHING,
    RESULT,
    ERROR
}

private open class MonoSubscriber<T> : Subscriber<T> {
    private var result: T? = null
    private var exception: Throwable? = null
    private var received = MonoReceived.NOTHING

    final override fun onSubscribe(subscription: Subscription) {
        subscription.request(1L)
    }

    final override fun onNext(element: T) {
        if (received == MonoReceived.RESULT) {
            throw IllegalStateException("Mono contract violated: Received more than one value")
        }

        if (received == MonoReceived.ERROR) {
            throw IllegalStateException("Mono contract violated: Received value after error")
        }

        received = MonoReceived.RESULT
        result = element
    }

    final override fun onError(error: Throwable) {
        if (received == MonoReceived.RESULT) {
            throw IllegalStateException("Mono contract violated: Received error after value")
        }

        received = MonoReceived.ERROR
        exception = error

        onReceiveError(error)
    }

    final override fun onComplete() {
        if (received == MonoReceived.NOTHING) {
            throw IllegalStateException("Mono contract violated: Received completion before any value")
        }

        if (received == MonoReceived.ERROR) {
            throw IllegalStateException("Mono contract violated: Received completion after error")
        }

        @Suppress("UNCHECKED_CAST")
        onReceive(result as T)
    }

    open fun onReceive(element: T) {

    }

    open fun onReceiveError(error: Throwable) {
        throw error
    }

    fun resultOrThrow(): T {
        @Suppress("UNCHECKED_CAST")
        return when (received) {
            MonoReceived.NOTHING -> throw NoSuchElementException("No value received yet")
            MonoReceived.RESULT -> result as T
            MonoReceived.ERROR -> throw NoSuchElementException("Received error instead of value")
        }
    }

    fun errorOrThrow(): Throwable {
        return when (received) {
            MonoReceived.NOTHING -> throw NoSuchElementException("No value received yet")
            MonoReceived.RESULT -> throw NoSuchElementException("No exception received")
            MonoReceived.ERROR -> exception!!
        }
    }
}

private class ListeningMonoSubscriber<T>(
    val receive: ((T) -> Unit)? = null,
    val receiveError: ((Throwable) -> Unit)? = null,
) : MonoSubscriber<T>() {
    override fun onReceive(element: T) {
        if (receive != null) {
            receive(element)
        }
    }

    override fun onReceiveError(error: Throwable) {
        if (receiveError != null) {
            receiveError(error)
        } else {
            super.onReceiveError(error)
        }
    }
}

private class ContinuingMonoSubscriber<T>(
    val continuation: Continuation<T>
) : MonoSubscriber<T>() {
    override fun onReceive(element: T) {
        continuation.resume(element)
    }

    override fun onReceiveError(error: Throwable) {
        continuation.resumeWithException(error)
    }
}