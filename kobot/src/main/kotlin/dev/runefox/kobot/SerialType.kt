package dev.runefox.kobot

import kotlinx.serialization.json.*
import kotlin.reflect.*

@RawSerialObjectApi
open class SerialType<T : Any> internal constructor(
    constructor: (JsonElement) -> T,
    internal val deconstructor: (T) -> JsonElement
) : (JsonElement) -> T by constructor {
    fun toJson(value: T): JsonElement {
        return deconstructor(value)
    }

    fun fromJson(value: JsonElement): T {
        return this(value)
    }

    @RawSerialObjectApi
    val list by lazy { list(this) }

    @RawSerialObjectApi
    val set by lazy { set(this) }

    @RawSerialObjectApi
    val map by lazy { map(this) }

    @RawSerialObjectApi
    companion object {
        val int = primitive({ it.int }, { JsonPrimitive(it) })
        val long = primitive({ it.long }, { JsonPrimitive(it) })
        val float = primitive({ it.float }, { JsonPrimitive(it) })
        val double = primitive({ it.double }, { JsonPrimitive(it) })
        val string = primitive({ it.content }, { JsonPrimitive(it) })
        val boolean = primitive({ it.boolean }, { JsonPrimitive(it) })

        val jsonElement = SerialType({ it }, { it })
        val jsonObject = SerialType({ it.jsonObject }, { it })
        val jsonArray = SerialType({ it.jsonArray }, { it })
        val jsonPrimitive = SerialType({ it.jsonPrimitive }, { it })

        private fun <T : Any> ofCompanion(type: KClass<T>): SerialType<T>? {
            for (nested in type.nestedClasses) {
                val inst = nested.objectInstance
                if (inst is SerialType<*>) {
                    @Suppress("UNCHECKED_CAST")
                    return inst as SerialType<T>
                }
            }

            return null
        }

        private fun validateType(proj: KTypeProjection): KType? {
            return when (proj.variance) {
                KVariance.INVARIANT, KVariance.OUT -> proj.type!!
                null, KVariance.IN -> null
            }
        }

        /**
         * Derives the [SerialType] of `T` through reflection.
         *
         * A [SerialType] is derived as follows:
         * - If `T` is [Int], [Long], [Float], [Double], [String], [Boolean], [JsonElement], [JsonObject], [JsonArray] or [JsonPrimitive], then the
         *   predefined [SerialType] is selected that belongs to `T`.
         * - If `T` is a `List<U>` for some type `U`, then the type of `U` is derived recursively, and then mapped as per [SerialType.list].
         * - If `T` is a `Set<U>` for some type `U`, then the type of `U` is derived recursively, and then mapped as per [SerialType.set].
         * - If `T` is a `Map<String, U>` for some type `U`, then the type of `U` is derived recursively, and then mapped as per [SerialType.map].
         * - Otherwise, the class of `T` is scanned for a nested object (possibly a companion object) that extends [SerialType].
         *
         * Note that if any type declares multiple objects that implement [SerialType], then an arbitrary one is picked. Which one specifically
         * is undefined and may even vary for each invocation. Hence, never declare more than one [SerialType] as nested object. If you still need
         * to have a second [SerialType] for your class, then declare it as a nested class and create an instance in your companion object.
         */
        inline fun <reified T : Any> derive(): SerialType<T> {
            @Suppress("UNCHECKED_CAST")
            return derive(typeOf<T>()) as SerialType<T>
        }

        inline fun <reified T : Any> deriveOrNull(): SerialType<T>? {
            @Suppress("UNCHECKED_CAST")
            return deriveOrNull(typeOf<T>()) as SerialType<T>?
        }

        fun derive(type: KType): SerialType<*> {
            return deriveOrNull(type) ?: throw IllegalArgumentException("Failed to derive SerialType of $type")
        }

        fun deriveOrNull(type: KType): SerialType<*>? {
            return when (val cls = type.classifier) {
                Int::class -> int
                Long::class -> long
                Float::class -> float
                Double::class -> double
                String::class -> string
                Boolean::class -> boolean

                JsonElement::class -> jsonElement
                JsonObject::class -> jsonObject
                JsonArray::class -> jsonArray
                JsonPrimitive::class -> jsonPrimitive

                List::class -> deriveOrNull(validateType(type.arguments[0]) ?: return null)?.list
                Set::class -> deriveOrNull(validateType(type.arguments[0]) ?: return null)?.set
                Map::class -> deriveOrNull(
                    if (validateType(type.arguments[0])?.classifier == String::class)
                        validateType(type.arguments[1]) ?: return null
                    else return null
                )?.map

                is KClass<*> -> ofCompanion(cls)
                else -> null
            }
        }

        @RawSerialObjectApi
        fun <T : Any> list(base: SerialType<T>): SerialType<List<T>> {
            return SerialType(
                { it.jsonArray.map(base) },
                { JsonArray(it.map(base.deconstructor)) }
            )
        }

        @RawSerialObjectApi
        fun <T : Any> set(base: SerialType<T>): SerialType<Set<T>> {
            return SerialType(
                { it.jsonArray.mapTo(mutableSetOf(), base).toSet() },
                { JsonArray(it.map(base.deconstructor)) }
            )
        }

        @RawSerialObjectApi
        fun <T : Any> map(base: SerialType<T>): SerialType<Map<String, T>> {
            return SerialType(
                { it.jsonObject.mapValues { (_, v) -> base(v) } },
                { JsonObject(it.mapValues { (_, v) -> base.deconstructor(v) }) }
            )
        }

        @RawSerialObjectApi
        private fun <T : Any> primitive(factory: (JsonPrimitive) -> T, deconstructor: (T) -> JsonPrimitive): SerialType<T> {
            return object : SerialPrimitiveType<T>(factory, deconstructor) {}
        }
    }
}

@RawSerialObjectApi
abstract class SerialCustomType<T : Any>(
    constructor: (JsonElement) -> T,
    deconstructor: (T) -> JsonElement
) : SerialType<T>(constructor, deconstructor)

@RawSerialObjectApi
abstract class SerialPrimitiveType<T : Any>(
    constructor: (JsonPrimitive) -> T,
    deconstructor: (T) -> JsonPrimitive
) : SerialType<T>({ constructor(it.jsonPrimitive) }, deconstructor)

@RawSerialObjectApi
abstract class SerialObjectType<T : SerialObject>(constructor: (JsonElement) -> T) : SerialType<T>(constructor, { it.toJson() })