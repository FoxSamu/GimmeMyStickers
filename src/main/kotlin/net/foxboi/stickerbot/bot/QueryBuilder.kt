package net.foxboi.stickerbot.bot

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