/*
 * Copyright (c) 2025 Olaf W. Nankman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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