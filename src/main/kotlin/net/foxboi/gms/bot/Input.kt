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

package net.foxboi.gms.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.foxboi.gms.Log
import net.foxboi.gms.util.Cell
import net.foxboi.gms.util.updateAndGet
import net.foxboi.gms.util.waitUntilIs

class Input() {
    private var state = Cell<RunState>(NotRunning.DISABLED_UNSTARTED)
    private val log = Log

    val enabled get() = state.value.enabled

    private inline fun update(newState: (RunState) -> RunState) {
        state.updateAndGet {
            val new = newState(it)
            if (new != it) {
                it.end()
            }
            new
        }.begin()
    }

    fun start() {
        log.debug { "Starting input" }
        update { it.started() }
    }

    fun stop() {
        log.debug { "Stopping input" }
        update { it.stopped() }
    }

    fun enable() {
        log.debug { "Enabling input" }
        update { it.enabled() }
    }

    fun disable() {
        log.debug { "Disabling input" }
        update { it.disabled() }
    }

    suspend fun getlnOrNull(): String? {
        return state.waitUntilIs<Running>().next()
    }

    suspend fun getln(): String {
        return getlnOrNull() ?: throw kotlinx.io.EOFException()
    }


    private interface RunState {
        fun begin()
        fun end()

        val enabled: Boolean
        fun started(): RunState
        fun stopped(): RunState
        fun enabled(): RunState
        fun disabled(): RunState
    }

    private enum class NotRunning : RunState {
        DISABLED_UNSTARTED,
        DISABLED_STARTED,
        ENABLED_UNSTARTED;

        override val enabled
            get() = this == ENABLED_UNSTARTED

        override fun begin() = Unit

        override fun end() = Unit

        override fun started() = when (this) {
            DISABLED_UNSTARTED -> DISABLED_STARTED
            DISABLED_STARTED -> DISABLED_STARTED
            ENABLED_UNSTARTED -> Running()
        }

        override fun stopped() = when (this) {
            DISABLED_UNSTARTED -> DISABLED_UNSTARTED
            DISABLED_STARTED -> DISABLED_UNSTARTED
            ENABLED_UNSTARTED -> ENABLED_UNSTARTED
        }

        override fun enabled() = when (this) {
            DISABLED_UNSTARTED -> ENABLED_UNSTARTED
            DISABLED_STARTED -> Running()
            ENABLED_UNSTARTED -> ENABLED_UNSTARTED
        }

        override fun disabled() = when (this) {
            DISABLED_UNSTARTED -> DISABLED_UNSTARTED
            DISABLED_STARTED -> DISABLED_STARTED
            ENABLED_UNSTARTED -> DISABLED_UNSTARTED
        }
    }

    private class Running : Thread(), RunState {
        private val queue = MutableSharedFlow<String?>()
        private var stop = false

        private val log = Log<Input>()

        init {
            isDaemon = true
        }

        override fun run() {
            runBlocking(Dispatchers.IO) {
                while (!stop) {
                    val elem = readlnOrNull()

                    queue.emit(elem)
                }
            }
        }

        override fun begin() = synchronized(this) {
            log.debug { "Input thread starting" }
            if (state == State.NEW) {
                start()
            }
        }

        override fun end() = synchronized(this) {
            log.debug { "Input thread stopping" }
            stop = true
            interrupt()
        }

        override val enabled get() = true

        override fun started() = this
        override fun stopped() = NotRunning.ENABLED_UNSTARTED
        override fun enabled() = this
        override fun disabled() = NotRunning.DISABLED_STARTED

        suspend fun next(): String? {
            return queue.first()
        }
    }
}