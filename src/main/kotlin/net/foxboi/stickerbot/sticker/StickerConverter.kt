package net.foxboi.stickerbot.sticker

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readByteArray
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

fun interface StickerConverter {
    suspend fun convert(from: Source, to: Sink)
}

object IdentityConverter : StickerConverter {
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

object WebpConverter : ImageConverter(EncodedImageFormat.WEBP)
object PngConverter : ImageConverter(EncodedImageFormat.PNG)
object JpegConverter : ImageConverter(EncodedImageFormat.JPEG, 90)
object BmpConverter : ImageConverter(EncodedImageFormat.BMP)