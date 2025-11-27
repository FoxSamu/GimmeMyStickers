package net.foxboi.gms.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class SignalTest {
    @Test
    fun testGetAfterSet(): Unit = runBlocking {
        val future = Gate<Int>()

        launch {
            delay(1000)
            val value = future.wait()
            assertEquals(3, value)
        }

        launch {
            future.open(3)
        }
    }

    @Test
    fun testGetBeforeSet(): Unit = runBlocking {
        val future = Gate<Int>()

        launch {
            val value = future.wait()
            assertEquals(3, value)
        }

        launch {
            delay(1000)
            future.open(3)
        }
    }

    @Test
    fun testMultiple(): Unit = runBlocking {
        val future = Gate<Int>()

        launch {
            val value = future.wait()
            assertEquals(3, value)
        }

        launch {
            delay(500)
            val value = future.wait()
            assertEquals(3, value)
        }

        launch {
            delay(1000)
            val value = future.wait()
            assertEquals(3, value)
        }

        launch {
            delay(500)
            future.open(3)
        }
    }

    @Test
    fun testDoubleSet(): Unit = runBlocking {
        val future = Gate<Int>()

        launch {
            future.open(3)
            delay(500)
            assertThrows<IllegalStateException> {
                future.open(6)
            }
        }
    }
}