package net.foxboi.stickerbot.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * A signal is a synchronization structure that allows coroutines to [wait] until the next call to [signal]. A coroutine calling [wait] will
 * suspend indefinitely until it is woken up by a call to [signal]. The call to [signal] can happen anywhere, even outside of coroutines.
 *
 * This class is notoriously similar to Java's [java.util.concurrent.locks.Condition] class. There are some major differences though:
 * - [Signal] does not require an associated lock or mutex to be locked in order to call [wait] or [signal].
 * - Calling [signal] always signals all waiting coroutines. There exists no method to signal just one waiting coroutine.
 * - Calling [wait] suspends the coroutine, instead of blocking the thread.
 *
 * One may see a signal as a traffic light for coroutines.
 * It is red by default, making all cars that come in wait (these are the coroutines calling [wait]). Once it turns green (via [signal]),
 * all waiting cars are allowed to continue. Once all waiting cars have passed, it goes back to red and the cycle repeats. In this analogy,
 * it is safe to assume that no cars come in while the light is green, simulating the fact that [signal] is instant.
 */
class Signal {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Suspends until the next call to [signal].
     *
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled.
     */
    suspend fun wait() {
        flow.first()
    }

    /**
     * Continues all coroutines that are currently waiting for this [Signal] through a call to [wait].
     * Coroutines that call [wait] after a call to [signal] will wait until another call to [signal] is made.
     */
    fun signal() {
        flow.tryEmit(Unit)
    }
}

suspend inline fun Signal.waitUntil(condition: () -> Boolean) {
    while (!condition()) {
        wait()
    }
}
