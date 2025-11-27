package net.foxboi.gms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun main() {
    withContext(Dispatchers.Default) {
        StickerBot.run()
    }
}