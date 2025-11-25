@file:OptIn(RawSerialObjectApi::class)

package dev.runefox.kobot.type

import dev.runefox.kobot.RawSerialObjectApi
import dev.runefox.kobot.SerialPrimitiveType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.long
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


/**
 * Either an [Id] or a [Name].
 */
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

    companion object : SerialPrimitiveType<IdOrName>(
        { if (it.isString) Name(it) else Id(it) },
        { if (it.isId) JsonPrimitive(it.id) else JsonPrimitive(it.name) }
    )
}

/**
 * An identifier, usually used to uniquely identify objects or to refer to them.
 *
 * @param id The identifier as a [Long] value.
 */
data class Id(
    override val id: Long
) : IdOrName {
    override fun toString(): String {
        return "[${id.toHexString()}]"
    }

    companion object : SerialPrimitiveType<Id>({ Id(it.long) }, { JsonPrimitive(it.id) })
}


/**
 * A name, usually a username.
 *
 * @param name The identifier as a [Long] value.
 */
data class Name(
    override val name: String
) : IdOrName {
    override fun toString(): String {
        return "@$name"
    }

    companion object : SerialPrimitiveType<Name>({ Name(it.content) }, { JsonPrimitive(it.name) })
}


/**
 * A UNIX timestamp, i.e. a point in time represented by an amount of (non-leap) seconds since
 * `1970-01-01 00:00:00 UTC` (the Epoch).
 *
 * @param secondsSinceEpoch The amount of seconds since the Epoch.
 */
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

    companion object : SerialPrimitiveType<Timestamp>({ Timestamp(it.long) }, { JsonPrimitive(it.secondsSinceEpoch) }) {
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