package net.foxboi.gms.bot.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.*

/**
 * A [Publisher] of zero or more elements.
 */
abstract class Many<T> : Publisher<T> {
    /**
     * Subscribes to this [Many] and calls the given callbacks accordingly.
     */
    open fun subscribe(
        onReceive: ((T) -> Unit)? = null,
        onComplete: (() -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null,
    ) {
        subscribe(ListeningManySubscriber(
            receive = onReceive,
            receiveCompletion = onComplete,
            receiveError = onError
        ))
    }

    open suspend fun collect(into: MutableCollection<in T>) {
        return suspendCoroutine { cont ->
            subscribe(CollectingManySubscriber(
                into,
                { add(it); this },
                {},
                cont
            ))
        }
    }

    open suspend fun collectList(): List<T> {
        return suspendCoroutine { cont ->
            subscribe(CollectingManySubscriber.list(cont))
        }
    }

    open suspend fun collectSet(): Set<T> {
        return suspendCoroutine { cont ->
            subscribe(CollectingManySubscriber.set(cont))
        }
    }

    open fun flow(): Flow<T> {
        return flow {
            FlowingManySubscriber(this)
                .subscribeAndSuspend(this@Many)
        }
    }

    companion object {
        /**
         * Returns a [Many] that supplies no values.
         */
        fun <T> empty(): Many<T> {
            @Suppress("UNCHECKED_CAST") // We can do this because with no values there are no types
            return DirectEmptyMany as Many<T>
        }

        /**
         * Returns a [Many] that directly supplies the given value.
         */
        fun <T> direct(value: T): Many<T> {
            return DirectSingletonMany(value)
        }

        /**
         * Returns a [Many] that directly supplies the given values.
         */
        fun <T> direct(vararg values: T): Many<T> {
            return DirectIterableMany(listOf(*values))
        }

        /**
         * Returns a [Many] that directly supplies the given values.
         */
        fun <T> direct(values: Iterable<T>): Many<T> {
            return DirectIterableMany(values.toList())
        }

        /**
         * Returns a [Many] that produces nothing suspendingly, and completes only when the given producer is done.
         */
        fun <T> produceEmpty(
            scope: CoroutineScope,
            producer: suspend CoroutineScope.() -> Unit
        ): Many<T> {
            return ProduceEmptyMany(scope, producer)
        }

        /**
         * Returns a [Many] that produces nothing suspendingly, and completes only when the given producer is done.
         */
        fun <T> produceEmpty(
            context: CoroutineContext,
            producer: suspend CoroutineScope.() -> Unit
        ): Many<T> {
            return ProduceEmptyMany(CoroutineScope(context), producer)
        }

        /**
         * Returns a [Many] that produces nothing suspendingly in the current coroutine context
         *, and completes only when the given producer is done.
         */
        suspend fun <T> produceEmpty(
            producer: suspend CoroutineScope.() -> Unit
        ): Many<T> {
            return ProduceEmptyMany(CoroutineScope(currentCoroutineContext()), producer)
        }

        /**
         * Returns a [Many] that produces a single value suspendingly.
         */
        fun <T> produceSingle(
            scope: CoroutineScope,
            producer: suspend CoroutineScope.() -> T
        ): Many<T> {
            return ProduceSingletonMany(scope, producer)
        }

        /**
         * Returns a [Many] that produces a single value suspendingly.
         */
        fun <T> produceSingle(
            context: CoroutineContext,
            producer: suspend CoroutineScope.() -> T
        ): Many<T> {
            return ProduceSingletonMany(CoroutineScope(context), producer)
        }

        /**
         * Returns a [Many] that produces a single value suspendingly in the current coroutine.
         */
        suspend fun <T> produceSingle(
            producer: suspend CoroutineScope.() -> T
        ): Many<T> {
            return ProduceSingletonMany(CoroutineScope(currentCoroutineContext()), producer)
        }

        /**
         * Returns a [Many] that produces an [Iterable] of values suspendingly.
         */
        fun <T> produceIterable(
            scope: CoroutineScope,
            producer: suspend CoroutineScope.() -> Iterable<T>
        ): Many<T> {
            return ProduceIterableMany(scope, producer)
        }

        /**
         * Returns a [Many] that produces an [Iterable] of values suspendingly.
         */
        fun <T> produceIterable(
            context: CoroutineContext,
            producer: suspend CoroutineScope.() -> Iterable<T>
        ): Many<T> {
            return ProduceIterableMany(CoroutineScope(context), producer)
        }

        /**
         * Returns a [Many] that produces an [Iterable] of values suspendingly in the current coroutine.
         */
        suspend fun <T> produceIterable(
            producer: suspend CoroutineScope.() -> Iterable<T>
        ): Many<T> {
            return ProduceIterableMany(CoroutineScope(currentCoroutineContext()), producer)
        }
    }
}

private object DirectEmptyMany : Many<Any?>() {
    override fun subscribe(subscriber: Subscriber<Any?>) {
        subscriber.onSubscribe(EmptySubscription(subscriber))
    }
}

private class DirectSingletonMany<T>(
    val value: T
) : Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(SingleElementSubscription(subscriber, value))
    }
}

private class DirectIterableMany<T>(
    val values: Iterable<T>
) : Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(IteratorSubscription(subscriber, values.iterator()))
    }
}

private class ProduceEmptyMany<T>(val scope: CoroutineScope, val producer: suspend CoroutineScope.() -> Unit) :
    Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(CoroutineEmptyProducerSubscription(subscriber, scope, producer))
    }
}

private class ProduceSingletonMany<T>(val scope: CoroutineScope, val producer: suspend CoroutineScope.() -> T) :
    Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(CoroutineSingletonProducerSubscription(subscriber, scope, producer))
    }
}

private class ProduceIterableMany<T>(
    val scope: CoroutineScope,
    val producer: suspend CoroutineScope.() -> Iterable<T>
) : Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(CoroutineIterableProducerSubscription(subscriber, scope, producer))
    }
}

private class ProducerMany<T>(
    val scope: CoroutineScope,
    val producer: suspend CoroutineScope.(suspend (T) -> Unit) -> Unit
) : Many<T>() {
    override fun subscribe(subscriber: Subscriber<T>) {
        subscriber.onSubscribe(CoroutineSingletonProduceMultipleSubscription(subscriber, scope, producer))
    }
}



// ==============================================================================================
// Default subscriber
// ==============================================================================================

private enum class ManyStatus {
    OK,
    COMPLETE,
    ERROR
}

private open class ManySubscriber<T> : Subscriber<T> {
    protected var status = ManyStatus.OK
    private lateinit var subscription: Subscription

    final override fun onSubscribe(subscription: Subscription) {
        this.subscription = subscription

        subscription.request(1L)
    }

    final override fun onNext(element: T) {
        if (status == ManyStatus.COMPLETE) {
            throw IllegalStateException("Many contract violated: Received value after completion")
        }

        if (status == ManyStatus.ERROR) {
            throw IllegalStateException("Many contract violated: Received value after error")
        }

        subscription.request(1L)
        onReceive(element)
    }

    final override fun onError(error: Throwable) {
        if (status == ManyStatus.COMPLETE) {
            throw IllegalStateException("Mono contract violated: Received error after completion")
        }

        status = ManyStatus.ERROR

        onReceiveError(error)
    }

    final override fun onComplete() {
        if (status == ManyStatus.ERROR) {
            throw IllegalStateException("Mono contract violated: Received completion after error")
        }

        status = ManyStatus.COMPLETE

        onReceiveCompletion()
    }

    open fun onReceive(element: T) {

    }

    open fun onReceiveCompletion() {

    }

    open fun onReceiveError(error: Throwable) {
        throw error
    }
}

private class ListeningManySubscriber<T>(
    val receive: ((T) -> Unit)? = null,
    val receiveCompletion: (() -> Unit)? = null,
    val receiveError: ((Throwable) -> Unit)? = null,
) : ManySubscriber<T>() {
    override fun onReceive(element: T) {
        if (receive != null) {
            receive(element)
        }
    }

    override fun onReceiveCompletion() {
        if (receiveCompletion != null) {
            receiveCompletion()
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

private class CollectingManySubscriber<T, I, R>(
    private var base: I,
    private val append: I.(T) -> I,
    private val finish: I.() -> R,
    private val continuation: Continuation<R>
) : ManySubscriber<T>() {
    override fun onReceive(element: T) {
        base = append(base, element)
    }

    override fun onReceiveCompletion() {
        continuation.resume(finish(base))
    }

    override fun onReceiveError(error: Throwable) {
        continuation.resumeWithException(error)
    }

    companion object {
        fun <T> list(continuation: Continuation<List<T>>): CollectingManySubscriber<T, *, List<T>> {
            return CollectingManySubscriber(
                mutableListOf<T>(),
                { this += it; this },
                { toList() },
                continuation
            )
        }

        fun <T> set(continuation: Continuation<Set<T>>): CollectingManySubscriber<T, *, Set<T>> {
            return CollectingManySubscriber(
                mutableSetOf<T>(),
                { this += it; this },
                { toSet() },
                continuation
            )
        }
    }
}


private abstract class AsyncManySubscriber<T>() : ManySubscriber<T>() {
    private val queue = mutableListOf<T>()
    private var error: Throwable? = null
    private var done = false

    private val cond = SynchronizedCondition()

    suspend fun launch() {
        while (true) {
            // Wait for events
            while (queue.isEmpty() && !done) {
                cond.wait()
            }

            // Process queue
            val elems = synchronized(this) { // Can't suspend in sync block, take elements out and process later
                val elems = queue.toList()
                queue.clear()
                elems
            }

            for (elem in elems) {
                receiveSuspend(elem)
            }

            // Keep going if not done
            if (!done) {
                continue
            }

            // Process end
            if (error != null) {
                throw error!!
            }

            break
        }
    }

    suspend fun subscribeAndSuspend(publisher: Publisher<T>) {
        publisher.subscribe(this)
        launch()
    }

    override fun onReceive(element: T) {
        synchronized(this) {
            queue.add(element)
        }
        cond.signal()
    }

    override fun onReceiveCompletion() {
        synchronized(this) {
            done = true
        }
        cond.signal()
    }

    override fun onReceiveError(error: Throwable) {
        synchronized(this) {
            this.error = error
            done = true
        }
        cond.signal()
    }

    protected abstract suspend fun receiveSuspend(element: T)
}

private class FlowingManySubscriber<T>(
    val flow: FlowCollector<T>
) : AsyncManySubscriber<T>() {
    override suspend fun receiveSuspend(element: T) {
        flow.emit(element)
    }
}

