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

@file:Suppress("UNCHECKED_CAST")

package net.foxboi.gms.util

import kotlinx.coroutines.flow.*

/**
 * A gate is a [Checkpoint] that allows coroutines to [wait] until the gate is opened through a call to [open]. Coroutines that call
 * [wait] after a call to [open] will not suspend. A gate stores a single value, which is assigned with a call to [open] and returned from [wait].
 *
 * Once opened, a gate can be closed through a call to [close]. Calling [close] will discard the stored value and future calls to [wait] will suspend
 * until the gate is opened again.
 *
 * A gate is similar to a [java.util.concurrent.CompletableFuture] as it allows coroutines to wait for a value that is set later. However, [Gate]
 * implements this behaviour in a suspending manner, and allows the value to be unset again.
 */
class Gate<T> private constructor(state: State<T>) : Checkpoint<T> {
    private val flow = MutableStateFlow<State<T>>(state)

    /**
     * Creates a gate that is initially closed.
     */
    constructor() : this(Closed)

    /**
     * Creates a gate that is initially open.
     *
     * @param value The value that is returned from [wait] while the gate is open.
     */
    constructor(value: T) : this(Open(value))

    /**
     * A flag indicating whether the gate has been opened.
     */
    val opened get() = flow.value is Open<T>

    /**
     * A flag indicating whether the gate has been closed.
     */
    val closed get() = flow.value is Closed

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
    override suspend fun wait(): T {
        val it = flow.dropWhile { it !is Open }.first() as Open<T>
        return it.value
    }

    /**
     * Suspends until the gate is closed by a call to [close] or [tryClose]. If the gate is already closed, this method will return immediately.
     *
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
     */
    suspend fun waitClosed() {
        flow.dropWhile { it !is Closed }.first()
    }

    /**
     * Opens the gate, assigning the given value to the open gate. Coroutines that called [wait] will be continued and the given value is returned
     * from the [wait] call. Future calls to [wait] will immediately return the given value.
     *
     * A gate can only be opened once. Attempting to call [open] twice will result in an [IllegalStateException].
     *
     * @param value The value to return from all [wait] calls to this gate.
     * @throws IllegalStateException When the gate is already open.
     */
    fun open(value: T) {
        if (!flow.compareAndSet(Closed, Open(value))) {
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


    /**
     * Closes the gate. Coroutines that called [waitClosed] will be continued. Future calls to [waitClosed] will immediately return the given value.
     * Future calls to [wait] will suspend until the gate is reopened again. This method returns the value the gate was opened with.
     *
     * A gate can only be closed once. Attempting to close twice in a row without opening in between will result in an [IllegalStateException].
     *
     * @return The value that the gate was last opened with.
     * @throws IllegalStateException When the gate is already closed.
     */
    fun close(): T {
        val state = closeInternal() ?: throw IllegalStateException("Double close")
        return state.value
    }

    /**
     * Attempts to close the [Gate], as per [close]. However, unlike [close], this method will not throw when the gate is already closed.
     *
     * @return True if the gate was successfully closed. False if the gate was already closed.
     */
    fun tryClose(): Boolean {
        return closeInternal() != null
    }

    /**
     * Attempts to close the [Gate], as per [close]. However, unlike [close], this method will not throw when the gate is already closed.
     * This method returns the value the gate was opened with, or null if the gate was already closed. Note that if a gate accepts `null` values, and
     * it was opened with a `null` value, then this method will also return `null`.
     *
     * @return The value that the gate was opened with, or null if the gate was not open.
     */
    fun closeOrNull(): T? {
        return closeInternal()?.value
    }

    private fun closeInternal(): Open<T>? {
        return flow.getAndUpdate { Closed } as? Open<T>
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
