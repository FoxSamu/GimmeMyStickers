@file:Suppress("UNCHECKED_CAST")

package net.foxboi.stickerbot.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

/**
 * A barrier is a synchronization structure that allows coroutines to [wait] until the barrier is opened through a call to [open]. Coroutines that
 * call [wait] after a call to [open] will not suspend. A barrier is in essence a [Gate] that can be re-closed, and that has no value.
 *
 * One may see a barrier as temporarily closed road.
 * Traffic (coroutines calling [wait]) has to wait for the road to be reopened.
 * Once the road is reopened ([open]), it becomes available for all traffic. Future traffic no longer needs to wait, until it the road is re-closed.
 *
 * @param open Whether the barrier is open by default.
 */
class Barrier(open: Boolean = false) {
    private val flow = MutableStateFlow(open)

    /**
     * A flag indicating whether the barrier has been opened. This property be modified to open or close the gate:
     * - Setting this to `true` is equivalent to calling [tryOpen].
     * - Setting this to `false` is equivalent to calling [tryClose].
     */
    var opened
        get() = flow.value
        set(value) {
            if (value) tryOpen() else tryClose()
        }

    /**
     * Suspends until the barrier is opened by a call to [open] or [tryOpen]. If the barrier is already open, this method will return immediately.
     *
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
     */
    suspend fun wait() {
        flow.dropWhile { !it }.first()
    }

    /**
     * Opens the barrier. Coroutines that called [wait] will be continued. Future calls to [wait] will immediately return the given value.
     *
     * A barrier can only be opened once and has to be closed first before it can be opened agaion. Attempting to call [open] twice, without calling
     * [close] in between, will result in an [IllegalStateException].
     *
     * @throws IllegalStateException When the barrier is already open.
     */
    fun open() {
        if (!tryOpen()) {
            throw IllegalStateException("Double open")
        }
    }

    /**
     * Closes the barrier. After this, coroutines that call [wait] have to wait until the barrier is reopened.
     *
     * A barrier can only be closed once and has to be opened first before it can be closed again. Attempting to call [close] twice, without calling
     * [open] in between, will result in an [IllegalStateException].
     *
     * @throws IllegalStateException When the barrier is already open.
     */
    fun close() {
        if (!tryClose()) {
            throw IllegalStateException("Double close")
        }
    }

    /**
     * Attempts to open the barrier, as per [open]. However, unlike [open], this method will not throw when the barrier is already open.
     *
     * @return True if the barrier was successfully opened. False if the barrier was already open.
     */
    fun tryOpen(): Boolean {
        return flow.compareAndSet(false, true)
    }

    /**
     * Attempts to close the barrier, as per [close]. However, unlike [close], this method will not throw when the barrier is already closed.
     *
     * @return True if the barrier was successfully closed. False if the barrier was already closed.
     */
    fun tryClose(): Boolean {
        return flow.compareAndSet(true, false)
    }
}