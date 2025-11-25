package dev.runefox.kobot

import kotlin.reflect.KClass
import kotlin.reflect.KProperty


@RawSerialObjectApi
fun <T : Any> requiredProperty(cls: KClass<T>, type: SerialType<T>, name: String): SerialDelegate<T> {
    return StaticNonNullDelegate(cls, type, name = name)
}

@RawSerialObjectApi
fun <T : Any> requiredProperty(cls: KClass<T>, type: SerialType<T>, rawPropertyName: Boolean = false): SerialDelegate<T> {
    return StaticNonNullDelegate(cls, type, nameDeriver = if (rawPropertyName) NameDeriver.RAW else NameDeriver.SNAKE_CASE)
}

@RawSerialObjectApi
inline fun <reified T : Any> requiredProperty(type: SerialType<T>, name: String): SerialDelegate<T> {
    return requiredProperty(T::class, type, name)
}

@RawSerialObjectApi
inline fun <reified T : Any> requiredProperty(type: SerialType<T>, rawPropertyName: Boolean = false): SerialDelegate<T> {
    return requiredProperty(T::class, type, rawPropertyName)
}

@RawSerialObjectApi
inline fun <reified T : Any> requiredProperty(name: String): SerialDelegate<T> {
    return requiredProperty(T::class, SerialType.derive(), name)
}

@RawSerialObjectApi
inline fun <reified T : Any> requiredProperty(rawPropertyName: Boolean = false): SerialDelegate<T> {
    return requiredProperty(T::class, SerialType.derive(), rawPropertyName)
}

@RawSerialObjectApi
fun <T : Any> optionalProperty(cls: KClass<T>, type: SerialType<T>, name: String): SerialDelegate<T?> {
    return StaticNullableDelegate(cls, type, name = name)
}

@RawSerialObjectApi
fun <T : Any> optionalProperty(cls: KClass<T>, type: SerialType<T>, rawPropertyName: Boolean = false): SerialDelegate<T?> {
    return StaticNullableDelegate(cls, type, nameDeriver = if (rawPropertyName) NameDeriver.RAW else NameDeriver.SNAKE_CASE)
}

@RawSerialObjectApi
inline fun <reified T : Any> optionalProperty(type: SerialType<T>, name: String): SerialDelegate<T?> {
    return optionalProperty(T::class, type, name)
}

@RawSerialObjectApi
inline fun <reified T : Any> optionalProperty(type: SerialType<T>, rawPropertyName: Boolean = false): SerialDelegate<T?> {
    return optionalProperty(T::class, type, rawPropertyName)
}

@RawSerialObjectApi
inline fun <reified T : Any> optionalProperty(name: String): SerialDelegate<T?> {
    return optionalProperty(T::class, SerialType.derive(), name)
}

@RawSerialObjectApi
inline fun <reified T : Any> optionalProperty(rawPropertyName: Boolean = false): SerialDelegate<T?> {
    return optionalProperty(T::class, SerialType.derive(), rawPropertyName)
}

@RawSerialObjectApi
fun <T : Any> defaultedProperty(cls: KClass<T>, type: SerialType<T>, default: T, name: String): SerialDelegate<T> {
    return StaticDefaultedDelegate(default, cls, type, name = name)
}

@RawSerialObjectApi
fun <T : Any> defaultedProperty(cls: KClass<T>, type: SerialType<T>, default: T, rawPropertyName: Boolean = false): SerialDelegate<T> {
    return StaticDefaultedDelegate(default, cls, type, nameDeriver = if (rawPropertyName) NameDeriver.RAW else NameDeriver.SNAKE_CASE)
}

@RawSerialObjectApi
inline fun <reified T : Any> defaultedProperty(type: SerialType<T>, default: T, name: String): SerialDelegate<T> {
    return defaultedProperty(T::class, type, default, name)
}

@RawSerialObjectApi
inline fun <reified T : Any> defaultedProperty(type: SerialType<T>, default: T, rawPropertyName: Boolean = false): SerialDelegate<T> {
    return defaultedProperty(T::class, type, default, rawPropertyName)
}

@RawSerialObjectApi
inline fun <reified T : Any> defaultedProperty(default: T, name: String): SerialDelegate<T> {
    return defaultedProperty(T::class, SerialType.derive(), default, name)
}

@RawSerialObjectApi
inline fun <reified T : Any> defaultedProperty(default: T, rawPropertyName: Boolean = false): SerialDelegate<T> {
    return defaultedProperty(T::class, SerialType.derive(), default, rawPropertyName)
}

@RawSerialObjectApi
interface SerialDelegate<T> {
    operator fun getValue(receiver: SerialObject, prop: KProperty<*>): T
    operator fun setValue(receiver: SerialObject, prop: KProperty<*>, value: T)
}

@RawSerialObjectApi
internal enum class NameDeriver(val deriver: (String) -> String) {
    RAW({ it }),
    SNAKE_CASE({
        buildString {
            for (c in it) {
                if (c.isUpperCase()) {
                    append('_')
                    append(c.lowercase())
                } else {
                    append(c)
                }
            }
        }
    })
}

@RawSerialObjectApi
internal abstract class DelegateInstance<T : Any>(
    val cls: KClass<T>,
    val type: SerialType<T>,
    var name: String? = null,
    val nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) {
    protected fun computeName(propName: String): String {
        val computedName = nameDeriver.deriver(propName)
        name = computedName
        return computedName
    }
}

@RawSerialObjectApi
internal class InnerNonNullDelegate<T : Any>(
    val obj: SerialObject,
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : DelegateInstance<T>(cls, type, name, nameDeriver) {
    private var instantiated = false
    private var value: T? = null

    fun get(prop: KProperty<*>): T = synchronized(obj) {
        val name = name ?: computeName(prop.name)
        if (!instantiated) {
            value = obj.raw(name, type)
            instantiated = true
        }
        return value!!
    }

    fun set(prop: KProperty<*>, value: T) = synchronized(obj) {
        this.value = value
        this.instantiated = true

        val name = name ?: computeName(prop.name)
        obj.putRaw(name, type, value)
    }
}

@RawSerialObjectApi
internal class InnerNullableDelegate<T : Any>(
    val obj: SerialObject,
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : DelegateInstance<T>(cls, type, name, nameDeriver) {
    private var instantiated = false
    private var value: T? = null

    @RawSerialObjectApi
    fun get(prop: KProperty<*>): T? = synchronized(obj) {
        val name = name ?: computeName(prop.name)
        if (!instantiated) {
            value = obj.rawOrNull(name, type)
            instantiated = true
        }
        return value
    }

    @RawSerialObjectApi
    fun set(prop: KProperty<*>, value: T?) = synchronized(obj) {
        this.value = value
        this.instantiated = true

        val name = name ?: computeName(prop.name)
        obj.putRawOrRemove(name, type, value)
    }
}

@RawSerialObjectApi
internal abstract class StaticDelegateInstance<T : Any, D : DelegateInstance<T>>(
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : DelegateInstance<T>(cls, type, name, nameDeriver) {
    protected inline fun <RD : DelegateInstance<*>> getDelegate(
        property: KProperty<*>,
        delegates: MutableMap<KProperty<*>, RD>,
        factory: () -> RD
    ): D {
        var delegate = delegates[property]
        if (delegate == null) {
            delegate = factory()
            delegates[property] = delegate
        }

        if (delegate.cls != cls) {
            throw IllegalStateException("Same property got a different type!")
        }

        @Suppress("UNCHECKED_CAST")
        return delegate as D
    }
}

@RawSerialObjectApi
internal class StaticNonNullDelegate<T : Any>(
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : StaticDelegateInstance<T, InnerNonNullDelegate<T>>(cls, type, name, nameDeriver), SerialDelegate<T> {
    override operator fun getValue(receiver: SerialObject, prop: KProperty<*>): T {
        return getDelegate(prop, receiver.nonNullDelegates) {
            InnerNonNullDelegate(receiver, cls, type, name, nameDeriver)
        }.get(prop)
    }

    override operator fun setValue(receiver: SerialObject, prop: KProperty<*>, value: T) {
        return getDelegate(prop, receiver.nonNullDelegates) {
            InnerNonNullDelegate(receiver, cls, type, name, nameDeriver)
        }.set(prop, value)
    }
}

@RawSerialObjectApi
internal class StaticNullableDelegate<T : Any>(
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : StaticDelegateInstance<T, InnerNullableDelegate<T>>(cls, type, name, nameDeriver), SerialDelegate<T?> {
    override operator fun getValue(receiver: SerialObject, prop: KProperty<*>): T? {
        return getDelegate(prop, receiver.nullableDelegates) {
            InnerNullableDelegate(receiver, cls, type, name, nameDeriver)
        }.get(prop)
    }

    override operator fun setValue(receiver: SerialObject, prop: KProperty<*>, value: T?) {
        return getDelegate(prop, receiver.nullableDelegates) {
            InnerNullableDelegate(receiver, cls, type, name, nameDeriver)
        }.set(prop, value)
    }
}

@RawSerialObjectApi
internal class StaticDefaultedDelegate<T : Any>(
    val default: T,
    cls: KClass<T>,
    type: SerialType<T>,
    name: String? = null,
    nameDeriver: NameDeriver = NameDeriver.SNAKE_CASE
) : StaticDelegateInstance<T, InnerNullableDelegate<T>>(cls, type, name, nameDeriver), SerialDelegate<T> {
    override operator fun getValue(receiver: SerialObject, prop: KProperty<*>): T {
        return getDelegate(prop, receiver.nullableDelegates) {
            InnerNullableDelegate(receiver, cls, type, name, nameDeriver)
        }.get(prop) ?: default
    }

    override operator fun setValue(receiver: SerialObject, prop: KProperty<*>, value: T) {
        return getDelegate(prop, receiver.nullableDelegates) {
            InnerNullableDelegate(receiver, cls, type, name, nameDeriver)
        }.set(prop, if (value == default) null else value)
    }
}