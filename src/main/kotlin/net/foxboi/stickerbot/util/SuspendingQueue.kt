package net.foxboi.stickerbot.util

import kotlinx.coroutines.sync.Mutex

class SuspendingQueue<T> {
    private val queue = ArrayDeque<T>()
    private val mutex = Mutex()
    private val condition = Condition()


}