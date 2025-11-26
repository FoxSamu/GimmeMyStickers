package net.foxboi.stickerbot.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionTest {
    @Test
    fun testCondition(): Unit = runBlocking {
        val cond = Signal()
        var value = 0

        launch {
            for (i in 1..10) {
                cond.wait()
                assertEquals(i, value)
            }
        }

        launch {
            cond.wait()
            assertEquals(1, value)
            cond.wait()
            assertEquals(2, value)
            cond.wait()
            assertEquals(3, value)
        }

        launch {
            for (i in 1..10) {
                delay(100)
                value++
                cond.signal()
            }
        }
    }

    @Test
    fun testConditionWithNoSubscriptions(): Unit = runBlocking {
        val cond = Signal()

        cond.signal()
        cond.signal()
    }
}