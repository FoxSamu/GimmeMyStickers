package net.foxboi.stickerbot.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.foxboi.stickerbot.bot.Id
import net.foxboi.stickerbot.storage.JsonStorable
import net.foxboi.stickerbot.storage.StorageLocation

class Session(
    val userId: Id,
    location: StorageLocation,
    json: Json = Json
) : JsonStorable<Session.Serial>(location, Serial.serializer(), json) {
    private var number: Int = 0

    fun number(): Int {
        return number
    }

    fun increment() {
        number++
        markDirty()
    }

    fun decrement() {
        number--
        markDirty()
    }

    override fun onInit() {
        number = 5
    }

    override fun onSave(): Serial {
        return Serial(number)
    }

    override fun onLoad(serial: Serial) {
        number = serial.number
    }

    companion object {
        fun filePath(id: Id): String {
            return id.id.toHexString()
        }
    }

    @Serializable
    class Serial(
        val number: Int
    )
}