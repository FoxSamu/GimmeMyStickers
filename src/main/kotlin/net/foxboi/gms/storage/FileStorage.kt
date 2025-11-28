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
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class FileStorage<K, V : Storable>(
    val root: Path,
    val keyToPath: (K) -> String,
    val factory: (K, StorageLocation) -> V,
    val fs: FileSystem = SystemFileSystem,
    val maxCached: Int = 65535
) : Storage<K, V> {
    private val cache = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
            if (size > maxCached) {
                eldest.value.save()
                return true
            }

            return false
        }
    }

    @Synchronized
    override fun get(key: K): V {
        return cache.getOrPut(key) {
            val path = Path(root, keyToPath(key))
            val location = Location(fs, path)

            val store = factory(key, location)
            store.load()
            store
        }
    }

    @Synchronized
    override fun flush() {
        for (v in cache.values) {
            v.save()
        }
    }

    @Synchronized
    override fun close() {
        flush()
        cache.clear()
    }

    private class Location(
        val fs: FileSystem,
        val path: Path
    ) : StorageLocation {
        override fun name(): String {
            return path.toString()
        }

        override fun exists(): Boolean {
            return fs.exists(path)
        }

        override fun source(): Source {
            return fs.source(path).buffered()
        }

        override fun sink(): Sink {
            val parent = path.parent
            if (parent != null) {
                fs.createDirectories(parent)
            }

            return fs.sink(path).buffered()
        }

        override fun delete() {
            fs.delete(path, mustExist = false)
        }
    }
}