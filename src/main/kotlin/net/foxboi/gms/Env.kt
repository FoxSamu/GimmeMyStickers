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

package net.foxboi.gms

object Env {
    private const val PREFIX = "GMS_"

    val token = this["TOKEN"]
    val storageDirectory = this["STORAGE_DIRECTORY", "."]
    val maxCachedSessions = this["MAX_CACHED_SESSIONS", "65535"].toIntOrNull() ?: 65535

    operator fun get(key: String, default: String? = null): String {
        return System.getenv(PREFIX + key)
            ?: default
            ?: throw Exception("Environment variable $key must be set in order to run")
    }
}