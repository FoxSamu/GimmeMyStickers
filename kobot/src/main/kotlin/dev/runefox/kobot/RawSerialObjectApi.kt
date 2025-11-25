package dev.runefox.kobot

import kotlin.annotation.AnnotationTarget.*

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Usage of raw object data API must be opted in with or propagated"
)
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    CLASS,
    ANNOTATION_CLASS,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPEALIAS
)
annotation class RawSerialObjectApi
