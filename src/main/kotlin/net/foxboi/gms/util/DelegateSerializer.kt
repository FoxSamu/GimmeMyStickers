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