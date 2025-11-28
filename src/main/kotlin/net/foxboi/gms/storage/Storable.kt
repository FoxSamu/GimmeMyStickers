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

package net.foxboi.gms.storage

import kotlinx.io.Sink
import kotlinx.io.Source
import net.foxboi.gms.Log

abstract class Storable(
    private val location: StorageLocation
) {
    private val log = Log

    private var changed = false

    val dirty get() = changed

    @Synchronized
    protected fun markDirty() {
        changed = true
    }

    @Synchronized
    protected fun markClean() {
        changed = false
    }

    @Synchronized
    fun save(force: Boolean = false) {
        if (force || changed) {
            location.sink().use {
                onSave(it)
            }
        }

        changed = false
    }

    @Synchronized
    fun load() {
        if (location.exists()) {
            try {
                location.source().use {
                    onLoad(it)
                }
                return
            } catch (e: Throwable) {
                log.error(e) { "Failed to load from store: ${location.name()}" }
            }
        }

        onInit()
        save(true) // Save immediately after init
    }

    @Synchronized
    fun delete() {
        location.delete()
    }

    protected abstract fun onInit()
    protected abstract fun onSave(output: Sink)
    protected abstract fun onLoad(input: Source)
}