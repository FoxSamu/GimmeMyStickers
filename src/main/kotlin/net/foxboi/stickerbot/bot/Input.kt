package net.foxboi.stickerbot.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.EOFException
import net.foxboi.stickerbot.util.Condition
import net.foxboi.stickerbot.util.withUnlock
import kotlin.concurrent.thread

class Input() {
    private val queue = ArrayDeque<String>()
    private val cond = Condition()
    private val mutex = Mutex()
    private var eof = false

    private val inputThread = thread(isDaemon = true) {
        runBlocking(Dispatchers.IO) {
            while (!eof) {
                val elem = readlnOrNull()

                if (elem == null) {
                    eof = true
                } else {
                    queue += elem
                }

                cond.signal()
            }
        }
    }

    suspend fun stop() {
        inputThread.interrupt()
        queue.clear()
        eof = true
        cond.signal()
    }

    suspend fun getlnOrNull(): String? {
        mutex.withLock {
            while (queue.isEmpty() && !eof) {
                mutex.withUnlock {
                    cond.wait()
                }
            }

            if (eof) {
                return null
            }

            return queue.removeFirst()
        }
    }

    suspend fun getln() = getlnOrNull() ?: throw EOFException()
}