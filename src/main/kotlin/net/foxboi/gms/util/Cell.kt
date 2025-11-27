@file:Suppress("UNCHECKED_CAST", "unused")

package net.foxboi.gms.util

import kotlinx.coroutines.flow.*

/**
 * A cell is a [Checkpoint] that stores a single value, and coroutines can wait for this value to change or for a certain condition to
 * be reached. Coroutines calling [wait] will be suspended until [value] is updated to a value that is not [equal][Any.equals] to the current value
 * (that is, a value `x` such that `x != value`). A cell is also an atomic variable.
 *
 * A cell is inherently the same construct as a [StateFlow], but it is simpler in nature and implements [Checkpoint].
 *
 * Note that when a cell stores a mutable object, mutations to that object will not cause coroutines to resume. The cell must be set to a different
 * object that is not equal to the current cell value in order to trigger the continuation of waiting coroutines.
 */
class Cell<T>(initial: T) : Checkpoint<T> {
    private val flow = MutableStateFlow(initial)
    private val flowReadOnly = flow.asStateFlow()

    /**
     * The value in the cell. Setting this value works the same as calling [set].
     */
    var value by flow::value

    /**
     * Suspends the calling coroutine until the cell's value changes to a value that is not equal to the current value.
     *
     * @return The new value.
     * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
     */
    override suspend fun wait(): T {
        val cur = flow.value
        return flow.dropWhile { it == cur }.first()
    }

    /**
     * Sets the cell's value. If the new value is not equal to the previous value, then coroutines that called [wait] will be resumed and the new
     * value is returned from the [wait] call.
     *
     * @param value The new value to be put in the cell.
     * @see compareAndSet
     * @see update
     */
    fun set(value: T) {
        flow.value = value
    }

    /**
     * Compares the cell's value to the expected value and updates it to a new value if the current value matches the expected value, in an atomic
     * fashion. That is, no thread can set the cell's value between comparing and updating.
     *
     * This method performs the following code atomically:
     * ```kotlin
     * if (this.value == expect) {
     *     set(update)
     *     return true
     * }
     *
     * return false
     * ```
     *
     * If the value is updated and the new value is not equal to the old value, then coroutines that called [wait] will be resumed and the new value
     * is returned from the [wait] call.
     *
     * This method returns `true` if the cell's value was set, even if it was set to a value that was equal to the old value. Thus, this method may
     * return `true` even if it didn't resume waiting coroutines.
     *
     * @param expect The value that was expected to be in the cell.
     * @param update The new value to be put in the cell.
     * @return True if the old value matched the expected value
     *
     * @see update
     */
    fun compareAndSet(expect: T, update: T): Boolean {
        return flow.compareAndSet(expect, update)
    }

    /**
     * Returns a [StateFlow] that flows the values set in this cell.
     */
    fun asFlow(): StateFlow<T> {
        return flowReadOnly
    }
}

/**
 * Suspends the calling coroutine until the given condition becomes true. If the condition is already true, this method will return immediately.
 *
 * Note that if the cell's value is a mutable object, and the condition becomes true due to the object mutating itself as such, then the calling
 * coroutine will not be resumed. The condition is only checked when the cell's value updates to a new value that is not equal to the current value.
 *
 * @return The value that matched the given condition.
 * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
 */
suspend inline fun <T> Cell<T>.waitUntil(condition: suspend (T) -> Boolean): T {
    var v = value
    while (!condition(v)) {
        v = wait()
    }

    return v
}

/**
 * Suspends the calling coroutine while the given condition is true. If the condition is already false, this method will return immediately.
 *
 * Note that if the cell's value is a mutable object, and the condition becomes false due to the object mutating itself as such, then the calling
 * coroutine will not be resumed. The condition is only checked when the cell's value updates to a new value that is not equal to the current value.
 *
 * @return The value that did not match the given condition.
 * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
 */
suspend inline fun <T> Cell<T>.waitWhile(condition: suspend (T) -> Boolean): T {
    var v = value
    while (condition(v)) {
        v = wait()
    }

    return v
}

/**
 * Suspends the calling coroutine until the cell's value becomes something other than `null`. If the cell's value is already not `null`, this method
 * will return immediately.
 *
 * @return The non-null value.
 * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
 */
suspend inline fun <T> Cell<T?>.waitUntilNotNull(): T {
    var v = value
    while (v == null) {
        v = wait()
    }

    return v
}

/**
 * Suspends the calling coroutine until the cell's value becomes `null`. If the cell's value is already `null`, this method
 * will return immediately.
 *
 * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
 */
suspend inline fun <T> Cell<T?>.waitUntilNull() {
    var v = value
    while (v != null) {
        v = wait()
    }
}

/**
 * Suspends the calling coroutine until the cell's value becomes an instance of `U`. If the cell's value is already an instance of `U`, this method
 * will return immediately.
 *
 * @return The value that is an instance of `U`, cast to `U`.
 * @throws kotlinx.coroutines.CancellationException When the calling coroutine is cancelled during the wait.
 */
suspend inline fun <reified U> Cell<*>.waitUntilIs(): U {
    var v = value
    while (v !is U) {
        v = wait()
    }

    return v
}

/**
 * Atomically updates the cell's value using the given function.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * set(function(this.value))
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call.
 *
 * Note that the given function may be called multiple times. This happens when another thread updates the value during the update
 * function. In this case, the update function will be called again with the new value. This will repeat until the update function succeeds
 * atomically.
 *
 * @param function The update function.
 */
inline fun <T> Cell<T>.update(function: (T) -> T) {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

/**
 * Atomically updates the cell's value using the given function, and returns the new value.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * set(function(this.value))
 * return this.value
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call.
 *
 * Note that the given function may be called multiple times. This happens when another thread updates the value during the update
 * function. In this case, the update function will be called again with the new value. This will repeat until the update function succeeds
 * atomically.
 *
 * @param function The update function.
 * @return The new value.
 */
inline fun <T> Cell<T>.updateAndGet(function: (T) -> T): T {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return nextValue
        }
    }
}

/**
 * Atomically updates the cell's value using the given function, and returns the old value.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * val oldValue = this.value
 * set(function(oldValue))
 * return oldValue
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call.
 *
 * Note that the given function may be called multiple times. This happens when another thread updates the value during the update
 * function. In this case, the update function will be called again with the new value. This will repeat until the update function succeeds
 * atomically.
 *
 * @param function The update function.
 * @return The old value.
 */
inline fun <T> Cell<T>.getAndUpdate(function: (T) -> T): T {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return prevValue
        }
    }
}

/**
 * Atomically updates the cell's value using the given function, and returns `true` if the old value is not equal to the new value.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * val oldValue = this.value
 * set(function(this.value))
 * return oldValue != this.value
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call. That is, if and only if this method returns `true`, coroutines resume from [wait][Cell.wait].
 *
 * Note that the given function may be called multiple times. This happens when another thread updates the value during the update
 * function. In this case, the update function will be called again with the new value. This will repeat until the update function succeeds
 * atomically.
 *
 * @param function The update function.
 * @return True if the new value is not equal to the old value.
 */
inline fun <T> Cell<T>.updateAndCompare(function: (T) -> T): Boolean {
    while (true) {
        val prevValue = value
        val nextValue = function(prevValue)
        if (compareAndSet(prevValue, nextValue)) {
            return prevValue != nextValue
        }
    }
}

/**
 * Atomically updates the cell's value if the new value is strictly greater than the current value. This method returns `true` if the value was
 * updated.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * if (value > this.value) {
 *     set(value)
 *     return true
 * }
 * return false
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call. Dueu to the nature of this method, coroutines resume from [wait][Cell.wait] if and only if this method returns `true`.
 *
 * @param value The potential new value.
 * @return True if the value was updated.
 */
fun <T : Comparable<T>> Cell<T>.setIfGreater(value: T): Boolean {
    return updateAndCompare { if (value > it) value else it }
}

/**
 * Atomically updates the cell's value if the new value is strictly less than the current value. This method returns `true` if the value was
 * updated.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * if (value < this.value) {
 *     set(value)
 *     return true
 * }
 * return false
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call. Dueu to the nature of this method, coroutines resume from [wait][Cell.wait] if and only if this method returns `true`.
 *
 * @param value The potential new value.
 * @return True if the value was updated.
 */
fun <T : Comparable<T>> Cell<T>.setIfLess(value: T): Boolean {
    return updateAndCompare { if (value < it) value else it }
}

/**
 * Atomically updates the cell's value if the new value is not equal to the current value. This method returns `true` if the value was
 * updated.
 *
 * This method performs the following code atomically:
 * ```kotlin
 * if (value != this.value) {
 *     set(value)
 *     return true
 * }
 * return false
 * ```
 *
 * If the new value is not equal to the old value, then coroutines that called [wait][Cell.wait] will be resumed and the new value is returned from
 * the [wait][Cell.wait] call. Dueu to the nature of this method, coroutines resume from [wait][Cell.wait] if and only if this method returns `true`.
 *
 * @param value The potential new value.
 * @return True if the value was updated.
 */
fun <T> Cell<T>.setIfDifferent(value: T): Boolean {
    return updateAndCompare { value }
}

/**
 * Atomically increments the cell's value, as per `update { it + 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("incrementInt")
fun Cell<Int>.increment() = update { it + 1 }

/**
 * Atomically increments the cell's value, as per `update { it + 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("incrementUInt")
fun Cell<UInt>.increment() = update { it + 1U }

/**
 * Atomically increments the cell's value, as per `update { it + 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("incrementLong")
fun Cell<Long>.increment() = update { it + 1L }

/**
 * Atomically increments the cell's value, as per `update { it + 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("incrementULong")
fun Cell<ULong>.increment() = update { it + 1UL }

/**
 * Atomically increments the cell's value and returns the new value, as per `updateAndGet { it + 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("incrementAndGetInt")
fun Cell<Int>.incrementAndGet() = updateAndGet { it + 1 }

/**
 * Atomically increments the cell's value and returns the new value, as per `updateAndGet { it + 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("incrementAndGetUInt")
fun Cell<UInt>.incrementAndGet() = updateAndGet { it + 1U }

/**
 * Atomically increments the cell's value and returns the new value, as per `updateAndGet { it + 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("incrementAndGetLong")
fun Cell<Long>.incrementAndGet() = updateAndGet { it + 1L }

/**
 * Atomically increments the cell's value and returns the new value, as per `updateAndGet { it + 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("incrementAndGetULong")
fun Cell<ULong>.incrementAndGet() = updateAndGet { it + 1UL }

/**
 * Atomically increments the cell's value and returns the old value, as per `getAndUpdate { it + 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndIncrementInt")
fun Cell<Int>.getAndIncrement() = getAndUpdate { it + 1 }

/**
 * Atomically increments the cell's value and returns the old value, as per `getAndUpdate { it + 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndIncrementUInt")
fun Cell<UInt>.getAndIncrement() = getAndUpdate { it + 1U }

/**
 * Atomically increments the cell's value and returns the old value, as per `getAndUpdate { it + 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndIncrementLong")
fun Cell<Long>.getAndIncrement() = getAndUpdate { it + 1L }

/**
 * Atomically increments the cell's value and returns the old value, as per `getAndUpdate { it + 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndIncrementULong")
fun Cell<ULong>.getAndIncrement() = getAndUpdate { it + 1UL }

/**
 * Atomically decrements the cell's value, as per `update { it - 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("decrementInt")
fun Cell<Int>.decrement() = update { it - 1 }

/**
 * Atomically decrements the cell's value, as per `update { it - 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("decrementUInt")
fun Cell<UInt>.decrement() = update { it - 1U }

/**
 * Atomically decrements the cell's value, as per `update { it - 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("decrementLong")
fun Cell<Long>.decrement() = update { it - 1L }

/**
 * Atomically decrements the cell's value, as per `update { it - 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see update
 */
@JvmName("decrementULong")
fun Cell<ULong>.decrement() = update { it - 1UL }

/**
 * Atomically decrements the cell's value and returns the new value, as per `updateAndGet { it - 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("decrementAndGetInt")
fun Cell<Int>.decrementAndGet() = updateAndGet { it - 1 }

/**
 * Atomically decrements the cell's value and returns the new value, as per `updateAndGet { it - 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("decrementAndGetUInt")
fun Cell<UInt>.decrementAndGet() = updateAndGet { it - 1U }

/**
 * Atomically decrements the cell's value and returns the new value, as per `updateAndGet { it - 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("decrementAndGetLong")
fun Cell<Long>.decrementAndGet() = updateAndGet { it - 1L }

/**
 * Atomically decrements the cell's value and returns the new value, as per `updateAndGet { it - 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see updateAndGet
 */
@JvmName("decrementAndGetULong")
fun Cell<ULong>.decrementAndGet() = updateAndGet { it - 1UL }

/**
 * Atomically decrements the cell's value and returns the old value, as per `getAndUpdate { it - 1 }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndDecrementInt")
fun Cell<Int>.getAndDecrement() = getAndUpdate { it - 1 }

/**
 * Atomically decrements the cell's value and returns the old value, as per `getAndUpdate { it - 1U }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndDecrementUInt")
fun Cell<UInt>.getAndDecrement() = getAndUpdate { it - 1U }

/**
 * Atomically decrements the cell's value and returns the old value, as per `getAndUpdate { it - 1L }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndDecrementLong")
fun Cell<Long>.getAndDecrement() = getAndUpdate { it - 1L }

/**
 * Atomically decrements the cell's value and returns the old value, as per `getAndUpdate { it - 1UL }`.
 * Since the new value is guaranteed to be never equal, this is guraranteed to resume coroutines that called [wait][Cell.wait].
 * @see getAndUpdate
 */
@JvmName("getAndDecrementULong")
fun Cell<ULong>.getAndDecrement() = getAndUpdate { it - 1UL }