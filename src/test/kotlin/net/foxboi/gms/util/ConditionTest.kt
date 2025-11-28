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
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionTest {
    @Test
    fun testCondition(): Unit = runBlocking {
        val cond = Signal<Unit>()
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
        val cond = Signal<Unit>()

        cond.signal()
        cond.signal()
    }
}