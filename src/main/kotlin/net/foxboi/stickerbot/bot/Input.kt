package net.foxboi.stickerbot.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.io.EOFException
import kotlin.concurrent.thread

class Input() {
    private val queue = MutableSharedFlow<String?>()
    private var stop = false

    private val inputThread = thread(isDaemon = true) {
        runBlocking(Dispatchers.IO) {
            while (!stop) {
                val elem = readlnOrNull()

                queue.emit(elem)
            }
        }
    }

    fun stop() {
        stop = true
        inputThread.interrupt()
    }

    suspend fun getlnOrNull(): String? {
        return queue.first()
    }

    suspend fun getln(): String {
        return getlnOrNull() ?: throw EOFException()
    }
}