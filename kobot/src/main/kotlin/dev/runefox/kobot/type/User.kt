@file:OptIn(RawSerialObjectApi::class)

package dev.runefox.kobot.type

import dev.runefox.kobot.*
import kotlinx.serialization.json.JsonElement

open class User : SerialObject {
    constructor(json: JsonElement) : super(json)

    constructor(
        id: Id,
        isBot: Boolean,
        firstName: String
    ) : super()
}

val User.id: Id by requiredProperty()
val User.isBot: Boolean by requiredProperty()
val User.firstName: String by requiredProperty()
val User.lastName: String? by optionalProperty()
val User.username: Name? by optionalProperty()
val User.languageCode: String? by optionalProperty()
val User.isPremium: Boolean by defaultedProperty(false)
val User.addedToAttachmentMenu: Boolean by defaultedProperty(false)

open class Me : User {
    constructor(json: JsonElement) : super(json)

    constructor(
        id: Id,
        isBot: Boolean,
        firstName: String
    ) : super(id, isBot, firstName)
}

val Me.canJoinGroups: Boolean by defaultedProperty(false)
val Me.canReadAllGroupMessages: Boolean by defaultedProperty(false)
val Me.supportsInlineQueries: Boolean by defaultedProperty(false)
val Me.canConnectToBusiness: Boolean by defaultedProperty(false)
val Me.hasMainWebApp: Boolean by defaultedProperty(false)
