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

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.serializer

class QueryBuilder(
    private val jsonObjectBuilder: JsonObjectBuilder,
    val json: Json
) {
    fun <T> put(name: String, serializer: SerializationStrategy<T>, value: T) {
        jsonObjectBuilder.put(name, json.encodeToJsonElement(serializer, value))
    }

    inline fun <reified T> put(name: String, value: T) {
        put(name, json.serializersModule.serializer(), value)
    }

    inline fun <reified T : Any> putMaybe(name: String, value: T?) {
        if (value != null) {
            put(name, json.serializersModule.serializer(), value)
        }
    }
}