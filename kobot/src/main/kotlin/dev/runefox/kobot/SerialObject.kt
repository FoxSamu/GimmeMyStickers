package dev.runefox.kobot

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KProperty

/**
 * An object that is internally represented as JSON data. This is the superclass of all objects returned from and supplied to the Telegram API.
 * Instead of deserializing and serializing such objects all at once, the object is simply a wrapper for the serialized data, which is deserialized
 * and serialized upon request. This allows other libraries to extend these objects with properties that may not have been implemented in the API yet.
 *
 * To allow your [SerialObject] class to be used as the type of a property of another [SerialObject], you must
 * declare a [SerialType] for your object. This is done by creating a nested object that extends [SerialObjectType].
 * You must declare this as an `object` nested in your [SerialObject] class, as it is found by reflection. It is
 * possible to use a `companion object` as [SerialType], this will be recognised. If multiple [SerialType] objects are
 * nested in your class, the behavior is undefined. See [SerialType.derive] for more details.
 *
 * It is recommended to not add properties to the [SerialObject] itself, unless they are transient. Add properties as extension functions using
 * supplied delegates. For example, creating a `Person` as a [SerialObject] is as follows:
 * ```kotlin
 * class Person : SerialObject {
 *     // Constructor to construct a Person from JSON data
 *     constructor(json: JsonElement) : super(json)
 *
 *     // Constructor to construct a new Person
 *     constructor(name: String, age: Int) : super() {
 *         this.name = name
 *         this.age = age
 *     }
 *
 *     // SerialType of Person
 *     object Type : SerialObjectType<Person>(::Person)
 * }
 *
 * // Serial properties: note that requiredProperty() will derive
 * // the SerialType.
 *
 * val Person.name: String by requiredProperty()
 * val Person.age: Int by requiredProperty()
 * ```
 */
abstract class SerialObject protected constructor(json: JsonElement) {
    protected constructor() : this(JsonObject(mapOf()))

    private val map = json.jsonObject.toMutableMap()
    private val name = this::class.simpleName!!

    @RawSerialObjectApi
    internal val nonNullDelegates = mutableMapOf<KProperty<*>, InnerNonNullDelegate<*>>()

    @RawSerialObjectApi
    internal val nullableDelegates = mutableMapOf<KProperty<*>, InnerNullableDelegate<*>>()

    @RawSerialObjectApi
    @Synchronized
    fun toJson(): JsonElement {
        return JsonObject(map)
    }

    @RawSerialObjectApi
    @Synchronized
    fun hasField(key: String): Boolean {
        return key in map
    }

    @RawSerialObjectApi
    @Synchronized
    fun rawOrNull(key: String): JsonElement? {
        return map[key]
    }

    @RawSerialObjectApi
    @Synchronized
    fun raw(key: String): JsonElement {
        return rawOrNull(key) ?: throw NoSuchElementException("$name: missing field '$key'")
    }

    @RawSerialObjectApi
    @Synchronized
    fun putRaw(key: String, data: JsonElement) {
        map[key] = data
    }

    @RawSerialObjectApi
    @Synchronized
    fun putRawOrRemove(key: String, data: JsonElement?) {
        if (data != null) {
            map[key] = data
        } else {
            map.remove(key)
        }
    }

    @RawSerialObjectApi
    @Synchronized
    fun <T : Any> rawOrNull(key: String, type: SerialType<T>): T? {
        return rawOrNull(key)?.let(type)
    }

    @RawSerialObjectApi
    @Synchronized
    fun <T : Any> raw(key: String, type: SerialType<T>): T {
        return raw(key).let(type)
    }

    @RawSerialObjectApi
    @Synchronized
    fun <T : Any> putRaw(key: String, type: SerialType<T>, data: T) {
        putRaw(key, type.toJson(data))
    }

    @RawSerialObjectApi
    @Synchronized
    fun <T : Any> putRawOrRemove(key: String, type: SerialType<T>, data: T?) {
        putRawOrRemove(key, data?.let { type.toJson(it) })
    }

    override fun toString(): String {
        @OptIn(RawSerialObjectApi::class)
        return "$name ${toJson()}"
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        @OptIn(RawSerialObjectApi::class)
        return other === this || other != null && other::class == this::class && other is SerialObject && other.map == map
    }
}
