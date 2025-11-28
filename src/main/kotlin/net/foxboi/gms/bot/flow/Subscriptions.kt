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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class IteratorSubscription<T>(
    val subscriber: Subscriber<T>,
    val values: Iterator<T>
) : Subscription {
    private var terminal = false

    override fun request(amount: Long) {
        if (terminal) {
            return
        }

        var n = 0L
        while (n < amount) {
            val next = try {
                if (values.hasNext()) {
                    values.next()
                } else {
                    break
                }
            } catch (e: Throwable) {
                terminal = true
                subscriber.onError(e)
                return
            }

            subscriber.onNext(next)

            if (amount != Long.MAX_VALUE) {
                n++
            }
        }

        if (!values.hasNext()) {
            terminal = true
            subscriber.onComplete()
        }
    }

    override fun cancel() {
        // N/A
    }
}

internal class SingleElementSubscription<T>(
    val subscriber: Subscriber<T>,
    val value: T
) : Subscription {
    private var terminal = false

    override fun request(amount: Long) {
        if (terminal) {
            return
        }

        if (amount == 0L) {
            return
        }

        subscriber.onNext(value)

        terminal = true
        subscriber.onComplete()
    }

    override fun cancel() {
        // N/A
    }
}

internal class EmptySubscription(
    val subscriber: Subscriber<*>
) : Subscription {
    private var terminal = false

    override fun request(amount: Long) {
        if (terminal) {
            return
        }

        terminal = true
        subscriber.onComplete()
    }

    override fun cancel() {
        // N/A
    }
}


private class SkipError(val error: Throwable) : Throwable()

internal abstract class CoroutineSubscription<T>(
    private val subscriber: Subscriber<T>,
    private val scope: CoroutineScope
) : Subscription {
    private var job: Job? = null
    private var cancel = false

    suspend fun run() {
        try {
            doRun(scope)
        } catch (e: SkipError) {
            throw e.error
        } catch (e: Throwable) {
            return subscriber.onError(e)
        }

        subscriber.onComplete()
    }

    protected fun send(next: T) {
        try {
            subscriber.onNext(next)
        } catch (e: Throwable) {
            throw SkipError(e)
        }
    }

    private fun start() {
        if (job == null) {
            val job = scope.launch {
                run()
            }

            this.job = job

            if (cancel) {
                // Cancel job if subscription was cancelled before receiving Job instance
                job.cancel()
            }
        }
    }


    protected abstract suspend fun doRun(scope: CoroutineScope)
    protected abstract fun doRequest(amount: Long)

    override fun request(amount: Long) {
        synchronized(this) {
            start()
            doRequest(amount)
        }
    }

    override fun cancel() {
        if (!cancel) {
            cancel = true
            job?.cancel()
        }
    }
}

internal abstract class CoroutineProduceOnceSubscription<V, T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    private val producer: suspend CoroutineScope.() -> V
) : CoroutineSubscription<T>(subscriber, scope) {
    private var requested = false

    protected abstract fun applyProduct(product: V, output: (T) -> Unit)

    override suspend fun doRun(scope: CoroutineScope) {
        applyProduct(scope.producer()) {
            send(it)
        }
    }

    override fun doRequest(amount: Long) {
        if (amount <= 0L || requested) {
            return
        }

        requested = true
    }
}


internal class CoroutineEmptyProducerSubscription<T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    producer: suspend CoroutineScope.() -> Unit
) : CoroutineProduceOnceSubscription<Unit, T>(subscriber, scope, producer) {
    override fun applyProduct(product: Unit, output: (T) -> Unit) {
        // N/A
    }
}


internal class CoroutineSingletonProducerSubscription<T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    producer: suspend CoroutineScope.() -> T
) : CoroutineProduceOnceSubscription<T, T>(subscriber, scope, producer) {
    override fun applyProduct(product: T, output: (T) -> Unit) {
        output(product)
    }
}


internal class CoroutineIterableProducerSubscription<T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    producer: suspend CoroutineScope.() -> Iterable<T>
) : CoroutineProduceOnceSubscription<Iterable<T>, T>(subscriber, scope, producer) {
    override fun applyProduct(product: Iterable<T>, output: (T) -> Unit) {
        for (element in product) {
            output(element)
        }
    }
}


internal abstract class CoroutineProduceMultipleSubscription<V, T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    private val producer: suspend CoroutineScope.(suspend (V) -> Unit) -> Unit
) : CoroutineSubscription<T>(subscriber, scope) {
    private var requests = 0L
    private val condition = SynchronizedCondition()

    /**
     * Handle the reception of a new product from the producer.
     */
    private suspend fun receiveProduct(product: V) {
        // Wait until we need to generate more values
        while (requests == 0L) {
            condition.wait()
        }

        // Decrease request count
        if (requests > 0) {
            requests--
        }

        // Supply element
        applyProduct(product) {
            send(it)
        }
    }

    /**
     * Apply a received product to the subscriber.
     */
    protected abstract fun applyProduct(product: V, output: (T) -> Unit)

    /**
     * Run the producer
     */
    override suspend fun doRun(scope: CoroutineScope) {
        scope.producer(::receiveProduct)
    }

    override fun doRequest(amount: Long) {
        if (amount <= 0L) {
            return
        }

        if (amount == Long.MAX_VALUE) {
            requests = -1
        }

        requests += amount
        condition.signal()
    }
}


internal class CoroutineSingletonProduceMultipleSubscription<T>(
    subscriber: Subscriber<T>,
    scope: CoroutineScope,
    producer: suspend CoroutineScope.(suspend (T) -> Unit) -> Unit
) : CoroutineProduceMultipleSubscription<T, T>(subscriber, scope, producer) {
    override fun applyProduct(product: T, output: (T) -> Unit) {
        output(product)
    }
}