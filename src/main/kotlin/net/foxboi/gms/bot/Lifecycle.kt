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

/**
 * Listener interface for lifecycle events of a [Bot]. Set [Bot.lifecycleListener] to a [LifecycleListener] to receive lifecycle events.
 */
interface LifecycleListener {
    /**
     * Called by the [Bot] to indicate that it has reached its ready state. Bot calls can be made from this method.
     */
    suspend fun onReady(bot: Bot) {

    }

    /**
     * Called on occasion by the [Bot]. This can be used to occasionally save things. Bot calls can be made from this method.
     * The interval between calls to this method can be set with [Bot.occasionInterval].
     */
    suspend fun onOccasion(bot: Bot) {

    }

    /**
     * Called by the [Bot] when a line of input was received from the standard input. Bot calls can be made from this method.
     */
    suspend fun onInput(bot: Bot, input: String) {

    }

    /**
     * Called by the [Bot] to indicate that it has stopped.
     * Bot calls can still be made from this method, but the bot will not poll any new updates and will terminate once this method returns.
     */
    suspend fun onStop(bot: Bot) {

    }
}

object LifecycleIgnorer : LifecycleListener