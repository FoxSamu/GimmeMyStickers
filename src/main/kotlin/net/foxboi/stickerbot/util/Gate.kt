@file:Suppress("UNCHECKED_CAST")

package net.foxboi.stickerbot.util

import kotlinx.coroutines.flow.*

/**
 * A gate is a synchronization structure that allows coroutines to [wait] until the gate is opened through a call to [open]. Coroutines that call
 * [wait] after a call to [open] will not suspend. A gate stores a single value, which is assigned with a call to [open] and returned from [wait].
 * Note that a gate cannot be closed. Once it's open, it cannot be opened a second time and its assigned value is frozen.
 *
 * [Gate] is similar to [Signal] in the sense that it allows multiple coroutines to wait for a call from elsewhere. However, unlike a signal, a gate
 * does not allow multiple "calls from elsewhere" and instead lets all future coroutines continue immediately. Additionally, a gate has a state,
 * whereas a signal does not.
 *
 * A gate is also similar to a [java.util.concurrent.CompletableFuture] as it allows coroutines to wait for a value that is set later. However, [Gate]
 * is much simpler and only implements a suspending call to await the result. It is merely meant as a synchronization structure, not as a future.
 *
 * One may see a gate as an unfinished road.
 * It is closed by default as it is unfinished. Traffic (coroutines calling [wait]) has to wait for the road to be finished.
 * Once the road is finished ([open]), it becomes available for all traffic. Future traffic no longer needs to wait.
 */
class Gate<T> {
    private val flow = MutableStateFlow<State<T>>(Closed)

    /**
     * A flag indicating whether the gate has been opened.
     */
    val opened get() = flow.value is Open<T>

    /**
     * The value associated to the gate, or `null` if the gate is closed.
     *
     * Note that this will also be `null` when the gate was opened with an
     * explicit `null` value. These cases can be distinguished with [opened].
     */
    val value
        get() = when (val v = flow.value) {
            is Closed -> null
            is Open<T> -> v.value
        }

    /**
     * Suspends until the gate is opened by a call to [open] or [tryOpen]. If the gate is already open, this method will return immediately.
     *
     * @return The value set when the gate was opened.
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
     */
    suspend fun wait(): T {
        val it = flow.dropWhile { it is Closed }.first() as Open<T>
        return it.value
    }

    /**
     * Opens the gate, assigning the given value to the open gate. Coroutines that called [wait] will be continued the given value is returned
     * from the [wait] call. Future calls to [wait] will immediately return the given value.
     *
     * A gate can only be opened once. Attempting to call [open] twice will result in an [IllegalStateException].
     *
     * @param value The value to return from all [wait] calls to this gate.
     * @throws IllegalStateException When the gate is already open.
     */
    fun open(value: T) {
        if (!tryOpen(value)) {
            throw IllegalStateException("Double open")
        }
    }

    /**
     * Attempts to open the [Gate], as per [open]. However, unlike [open], this method will not throw when the gate is already open.
     *
     * @param value The value to return from all [wait] calls to this gate.
     * @return True if the gate was successfully opened. False if the gate was already open.
     */
    fun tryOpen(value: T): Boolean {
        return flow.compareAndSet(Closed, Open(value))
    }

    private sealed interface State<in T>
    private object Closed : State<Any?>
    private class Open<T>(val value: T) : State<T>
}


/**
 * Opens the gate, assigning [Unit] as value to the open gate. This is equivalent to calling [Gate.open] with [Unit] as parameter.
 *
 * A gate can only be opened once. Attempting to call [open] twice will result in an [IllegalStateException].
 *
 * @throws IllegalStateException When the gate is already open.
 */
fun Gate<Unit>.open() {
    open(Unit)
}

/**
 * Attempts to open the [Gate] with a [Unit] value, as per [open]. However, unlike [open], this method will not throw when the gate is already open.
 *
 * @return True if the gate was successfully opened. False if the gate was already open.
 */
fun Gate<Unit>.tryOpen(): Boolean {
    return tryOpen(Unit)
}

/**
 * Returns a [Flow] that will emit the value of the receiving gate once that gate opens. This is equivalent to `flow { emit(gate.wait()) }`.
 */
fun <T> Gate<T>.asFlow(): Flow<T> {
    return flow { emit(wait()) }
}
