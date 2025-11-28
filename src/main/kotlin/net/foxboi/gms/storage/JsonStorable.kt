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
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class JsonStorable<S>(
    location: StorageLocation,
    private val serializer: KSerializer<S>,
    protected val json: Json = Json
) : Storable(location) {
    final override fun onSave(output: Sink) {
        val str = json.encodeToString(serializer, onSave())
        output.writeString(str, Charsets.UTF_8)
    }

    final override fun onLoad(input: Source) {
        val str = input.readString(Charsets.UTF_8)
        onLoad(json.decodeFromString(serializer, str))
    }

    protected abstract fun onSave(): S
    protected abstract fun onLoad(serial: S)
}