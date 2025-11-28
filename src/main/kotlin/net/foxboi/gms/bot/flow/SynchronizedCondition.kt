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

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SynchronizedCondition {
    private val continuations = mutableListOf<CancellableContinuation<Unit>>()
    private val lock = ReentrantLock()

    suspend fun wait() {
        suspendCancellableCoroutine {
            lock.withLock {
                continuations += it
            }
        }
    }

    fun signal() {
        lock.withLock {
            for (cont in continuations) {
                cont.resume(Unit)
            }
            continuations.clear()
        }
    }

    fun signalException(e: Throwable) {
        lock.withLock {
            for (cont in continuations) {
                cont.resumeWithException(e)
            }
            continuations.clear()
        }
    }

    fun cancel(cause: Throwable? = null) {
        lock.withLock {
            for (cont in continuations) {
                cont.cancel(cause)
            }
            continuations.clear()
        }
    }
}