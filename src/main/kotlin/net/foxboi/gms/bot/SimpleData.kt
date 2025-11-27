package net.foxboi.gms.bot

import kotlinx.io.Buffer
import kotlinx.io.readByteString
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import net.foxboi.gms.util.DelegateSerializer
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
private class SerialIdOrName(
    val id: Long? = null,
    val name: String? = null
)

private object SerialIdOrNameSerializer : JsonTransformingSerializer<SerialIdOrName>(SerialIdOrName.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive) {
            return buildJsonObject {
                if (element.isString) {
                    put("name", element)
                } else {
                    put("id", element)
                }
            }
        }

        return buildJsonObject {}
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element is JsonObject) {
            return element["name"] ?: element["id"] ?: element
        }

        return element
    }
}

private object IdOrNameSerializer : DelegateSerializer<IdOrName, SerialIdOrName>(SerialIdOrNameSerializer) {
    override fun encode(value: IdOrName): SerialIdOrName {
        return SerialIdOrName(value.idOrNull, value.nameOrNull)
    }

    override fun decode(value: SerialIdOrName): IdOrName {
        return when {
            value.id != null -> Id(value.id)
            value.name != null -> Name(value.name)
            else -> throw SerializationException("IdOrName must be number or string")
        }
    }
}


/**
 * Either an [Id] or a [Name].
 */
@Serializable(IdOrNameSerializer::class)
sealed interface IdOrName {
    val isId get() = this is Id
    val isName get() = this is Name

    val asId get() = this as? Id ?: throw NoSuchElementException("IdOrName is a Name")
    val asName get() = this as? Name ?: throw NoSuchElementException("IdOrName is an Id")

    val asIdOrNull get() = this as? Id
    val asNameOrNull get() = this as? Name

    val id get() = asId.id
    val name get() = asName.name

    val idOrNull get() = asIdOrNull?.id
    val nameOrNull get() = asNameOrNull?.name
}

/**
 * An identifier, usually used to uniquely identify objects or to refer to them.
 *
 * @param id The identifier as a [Long] value.
 */
@Serializable(IdSerializer::class)
data class Id(
    override val id: Long
) : IdOrName {
    val bytes by lazy {
        val buf = Buffer()
        buf.writeLong(id)
        buf.readByteString()
    }

    override fun toString(): String {
        return "[${id.toHexString()}]"
    }
}

private object IdSerializer : DelegateSerializer<Id, Long>(Long.serializer()) {
    override fun encode(value: Id) = value.id
    override fun decode(value: Long) = Id(value)
}


/**
 * A name, usually a username.
 *
 * @param name The identifier as a [Long] value.
 */
@Serializable(NameSerializer::class)
data class Name(
    override val name: String
) : IdOrName {
    override fun toString(): String {
        return "@$name"
    }
}

private object NameSerializer : DelegateSerializer<Name, String>(String.serializer()) {
    override fun encode(value: Name) = value.name
    override fun decode(value: String) = Name(value)
}


/**
 * A UNIX timestamp, i.e. a point in time represented by an amount of (non-leap) seconds since
 * `1970-01-01 00:00:00 UTC` (the Epoch).
 *
 * @param secondsSinceEpoch The amount of seconds since the Epoch.
 */
@Serializable(TimestampSerializer::class)
data class Timestamp(
    val secondsSinceEpoch: Long
) {
    /**
     * Returns `true` when this timestamp is `1970-01-01 00:00:00 UTC`, that is, [secondsSinceEpoch] is 0.
     */
    fun isEpoch() = secondsSinceEpoch == 0L

    /**
     * Converts this [Timestamp] to an [Instant].
     */
    @ExperimentalTime
    fun toInstant(): Instant {
        return Instant.fromEpochSeconds(secondsSinceEpoch)
    }

    companion object {
        /**
         * The Epoch, i.e. the timestamp representing `1970-01-01 00:00:00 UTC`.
         */
        val epoch = Timestamp(0L)

        /**
         * Returns a [Timestamp] of the current time.
         */
        @ExperimentalTime
        fun now(clock: Clock = Clock.System): Timestamp {
            return clock.now().toTimestamp()
        }
    }
}

/**
 * Converts this [Instant] to a [Timestamp]. The [Instant] is rounded down to seconds, so any nanoseconds are simply
 * discarded.
 */
@ExperimentalTime
fun Instant.toTimestamp(): Timestamp {
    return Timestamp(epochSeconds)
}

private object TimestampSerializer : DelegateSerializer<Timestamp, Long>(Long.serializer()) {
    override fun encode(value: Timestamp) = value.secondsSinceEpoch
    override fun decode(value: Long) = Timestamp(value)
}