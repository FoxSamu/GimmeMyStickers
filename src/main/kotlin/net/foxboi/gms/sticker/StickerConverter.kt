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