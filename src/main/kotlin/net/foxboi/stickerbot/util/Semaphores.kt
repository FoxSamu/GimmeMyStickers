package net.foxboi.stickerbot.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

suspend inline fun <R> Mutex.withUnlock(action: () -> R) {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    unlock()
    try {
        action()
    } finally {
        lock()
    }
}

suspend inline fun <R> Semaphore.withRelease(action: () -> R) {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    release()
    try {
        action()
    } finally {
        acquire()
    }
}