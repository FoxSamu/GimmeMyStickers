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

package net.foxboi.gms.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * A signal is a [Checkpoint] that allows coroutines to [wait] until the next call to [signal]. A coroutine calling [wait] will
 * suspend indefinitely until it is woken up by a call to [signal]. The call to [signal] can happen anywhere, even outside of coroutines.
 *
 * The [signal] method accepts a single value, which is returned from the [wait] method in all of the resumed coroutines.
 *
 * This class is notoriously similar to Java's [java.util.concurrent.locks.Condition] class. There are some major differences though:
 * - [Signal] does not require an associated lock or mutex to be locked in order to call [wait] or [signal].
 * - Calling [signal] always signals all waiting coroutines. There exists no method to signal just one waiting coroutine.
 * - Calling [wait] suspends the coroutine, instead of blocking the thread.
 * - [Signal] allows attaching a value to [signal], to be returned from [wait].
 */
class Signal<T> : Checkpoint<T> {
    private val flow = MutableSharedFlow<T>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * Suspends until the next call to [signal].
     *
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled.
     */
    override suspend fun wait(): T {
        return flow.first()
    }

    /**
     * Continues all coroutines that are currently waiting for this [Signal] through a call to [wait].
     * Coroutines that call [wait] after a call to [signal] will wait until another call to [signal] is made.
     */
    fun signal(value: T) {
        flow.tryEmit(value)
    }
}

fun Signal<Unit>.signal() {
    signal(Unit)
}