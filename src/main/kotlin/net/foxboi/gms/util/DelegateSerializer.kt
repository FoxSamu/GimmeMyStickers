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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class DelegateSerializer<T, D>(val base: KSerializer<D>) : KSerializer<T> {
    override val descriptor = base.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        base.serialize(encoder, encode(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        return decode(base.deserialize(decoder))
    }

    abstract fun encode(value: T): D
    abstract fun decode(value: D): T
}