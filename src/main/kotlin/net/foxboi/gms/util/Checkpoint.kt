package net.foxboi.gms.util

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/**
 * A checkpoint is a synchronization structure that can be waited for. A checkpoint has one method, [wait], which suspends and then returns a value.
 * The specific way in which this happens is dependent on the chosen implementation.
 *
 * Checkpoints are similar to futures, in the sense that they provide a value that will arrive later. However, the contract of a future is more
 * restrictive, allowing a value to arrive once and requiring that all coroutines that attempt to get the value after it arrived will proceed
 * immediately. A checkpoint does not have these restrictions. The [wait] call can suspend even if it previously returned a value, and multiple calls
 * to [wait] may return different values. A future is, however, a type of checkpoint.
 *
 * Some implementations of [Checkpoint] may specifically implement `Checkpoint<Unit>` as they do not support supplying values, only the waiting.
 *
 * @see Signal
 * @see Gate
 * @see Cell
 */
interface Checkpoint<T> {
    /**
     * Wait for a value. This method may suspend but doesn't have to. This method may also throw an exception or throw
     * [kotlinx.coroutines.CancellationException] when the caller is cancelled. The behaviour of this method is up to the implementation of the
     * checkpoint.
     *
     * @return The awaited value.
     */
    suspend fun wait(): T
}

/**
 * Returns a checkpoint that returns [Unit] once this job is finished.
 */
fun Job.asCheckpoint(): Checkpoint<Unit> {
    return object : Checkpoint<Unit> {
        override suspend fun wait() = join()
    }
}

/**
 * Returns a checkpoint that returns the deferred value once this job is finished.
 */
fun <T> Deferred<T>.asCheckpoint(): Checkpoint<T> {
    return object : Checkpoint<T> {
        override suspend fun wait() = await()
    }
}