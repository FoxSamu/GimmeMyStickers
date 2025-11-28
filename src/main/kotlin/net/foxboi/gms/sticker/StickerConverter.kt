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

package net.foxboi.gms.sticker

import kotlinx.io.*
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.util.zip.GZIPInputStream

fun interface StickerConverter {
    suspend fun convert(from: Source, to: Sink)
}

object Copy : StickerConverter {
    override suspend fun convert(from: Source, to: Sink) {
        from.transferTo(to)
    }
}

abstract class ImageConverter(
    val to: EncodedImageFormat,
    val quality: Int = 100
) : StickerConverter {
    override suspend fun convert(from: Source, to: Sink) {
        val img = try {
            Image.makeFromEncoded(from.readByteArray())
        } catch (e: Exception) {
            throw IOException("Failed to load image", e)
        }

        val enc = img.encodeToData(this.to, this.quality)
            ?: throw IOException("Failed to convert image")

        to.write(enc.bytes)
    }
}

object ToWebp : ImageConverter(EncodedImageFormat.WEBP)
object ToPng : ImageConverter(EncodedImageFormat.PNG)
object ToJpeg : ImageConverter(EncodedImageFormat.JPEG, 90)
object ToBmp : ImageConverter(EncodedImageFormat.BMP)


object GUnzip : StickerConverter {
    override suspend fun convert(from: Source, to: Sink) {
        GZIPInputStream(from.asInputStream()).asSource().use {
            to.transferFrom(it)
        }
    }
}