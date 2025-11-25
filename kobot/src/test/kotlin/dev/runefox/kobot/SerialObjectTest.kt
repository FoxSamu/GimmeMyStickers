@file:OptIn(RawSerialObjectApi::class)

package dev.runefox.kobot

import dev.runefox.kobot.type.IdOrName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SerialObjectTest {
    @Test
    fun testPerson() {
        val person = Person("John Doe", 42)

        assertEquals("John Doe", person.name)
        assertEquals(42, person.age)
        assertEquals(null, person.height)
    }

    @Test
    fun testDerive() {
        val type = SerialType.derive<List<Map<String, IdOrName>>>()
        val construct = type(buildJsonArray {
            add(buildJsonObject {
                put("Foo", JsonPrimitive(3))
                put("Bar", JsonPrimitive(6))
                put("Baz", JsonPrimitive(9))
            })
            add(buildJsonObject {
                put("Foo", JsonPrimitive(1))
                put("Bar", JsonPrimitive(2))
                put("Baz", JsonPrimitive("Foo"))
                put("Gus", JsonPrimitive("Gus"))
            })
        })

        println(construct)
    }
}

class Person : SerialObject {
    constructor(json: JsonElement) : super(json)

    constructor(name: String, age: Int, height: Int? = null) : super() {
        this.name = name
        this.age = age
        this.height = height
    }

    companion object : SerialObjectType<Person>(::Person)
}

var Person.name by requiredProperty(SerialType.string)
var Person.age by requiredProperty(SerialType.int)
var Person.height by optionalProperty(SerialType.int)