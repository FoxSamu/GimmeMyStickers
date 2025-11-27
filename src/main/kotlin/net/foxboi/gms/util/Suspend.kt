package net.foxboi.gms.util

import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Suspends the calling coroutine until cancellation. This method can never complete normally, in fact, it cannot throw anything except
 * [kotlinx.coroutines.CancellationException].
 */
suspend fun suspendIndefinitely() = suspendCancellableCoroutine<Unit> {
    // N/A
}