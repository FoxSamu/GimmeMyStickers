package net.foxboi.stickerbot

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.foxboi.stickerbot.storage.JsonStorable
import net.foxboi.stickerbot.storage.StorageLocation

class Session(
    location: StorageLocation,
    json: Json = Json.Default
) : JsonStorable<Session.Serial>(location, Serial.serializer(), json) {
    private var desiredFormats = DesiredFormats()

    fun wantsPng() = desiredFormats.png
    fun wantsJpeg() = desiredFormats.jpeg
    fun wantsWebp() = desiredFormats.webp
    fun wantsBmp() = desiredFormats.bmp

    fun wants(png: Boolean = false, jpeg: Boolean = false, webp: Boolean = false, bmp: Boolean = false) {
        desiredFormats = DesiredFormats(
            png = png,
            jpeg = jpeg,
            webp = webp,
            bmp = bmp
        )

        markDirty()
    }

    fun wantsPng(want: Boolean) {
        desiredFormats = desiredFormats.copy(png = want)
        markDirty()
    }

    fun wantsJpeg(want: Boolean) {
        desiredFormats = desiredFormats.copy(jpeg = want)
        markDirty()
    }

    fun wantsWebp(want: Boolean) {
        desiredFormats = desiredFormats.copy(webp = want)
        markDirty()
    }

    fun wantsBmp(want: Boolean) {
        desiredFormats = desiredFormats.copy(bmp = want)
        markDirty()
    }


    private var filePrefix = ""

    fun filePrefix() = filePrefix

    fun filePrefix(prefix: String) {
        filePrefix = prefix
        markDirty()
    }


    private var fileCounter = 0UL

    fun fileCounter() = fileCounter

    fun incrementFileCounter() {
        fileCounter++
        markDirty()
    }

    fun resetFileCounter() {
        fileCounter = 0UL
        markDirty()
    }

    fun fileName(): String {
        return "$filePrefix$fileCounter"
    }


    override fun onInit() {
        desiredFormats = DesiredFormats(
            png = true
        )

        filePrefix = ""
        fileCounter = 0UL
    }

    override fun onSave(): Serial {
        return Serial(
            desiredFormats,
            filePrefix,
            fileCounter
        )
    }

    override fun onLoad(serial: Serial) {
        desiredFormats = serial.desiredFormats
        filePrefix = serial.filePrefix
        fileCounter = serial.fileCounter
    }

    @Serializable
    class Serial(
        val desiredFormats: DesiredFormats,
        val filePrefix: String,
        val fileCounter: ULong,
    )

    @Serializable
    data class DesiredFormats(
        val png: Boolean = false,
        val jpeg: Boolean = false,
        val webp: Boolean = false,
        val bmp: Boolean = false,
    )
}