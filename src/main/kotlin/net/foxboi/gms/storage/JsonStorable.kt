package net.foxboi.gms.storage

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

abstract class JsonStorable<S>(
    location: StorageLocation,
    private val serializer: KSerializer<S>,
    protected val json: Json = Json
) : Storable(location) {
    final override fun onSave(output: Sink) {
        val str = json.encodeToString(serializer, onSave())
        output.writeString(str, Charsets.UTF_8)
    }

    final override fun onLoad(input: Source) {
        val str = input.readString(Charsets.UTF_8)
        onLoad(json.decodeFromString(serializer, str))
    }

    protected abstract fun onSave(): S
    protected abstract fun onLoad(serial: S)
}