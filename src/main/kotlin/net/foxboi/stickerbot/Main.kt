package net.foxboi.stickerbot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main() {
    withContext(Dispatchers.Default) {
        StickerBot.run()
    }
}