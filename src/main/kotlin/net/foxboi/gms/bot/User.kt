/*
 * Copyright (c) 2025 Olaf W. Nankman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.foxboi.gms.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Requests and returns the user data of this bot user, as a [Me] object.
 *
 * [Telegram API Specification](https://core.telegram.org/bots/api#getme)
 *
 * @return A [Me] object containing the user data of this bot.
 */
suspend fun Bot.getMe(): Me {
    return call("getMe")
}



sealed interface BaseUser {
    val id: Id
    val isBot: Boolean
    val firstName: String
    val lastName: String?
    val username: Name?
    val languageCode: String?
    val isPremium: Boolean
    val addedToAttachmentMenu: Boolean
}

@Serializable
data class User(
    @SerialName("id")
    override val id: Id,

    @SerialName("is_bot")
    override val isBot: Boolean,

    @SerialName("first_name")
    override val firstName: String,

    @SerialName("last_name")
    override val lastName: String? = null,

    @SerialName("username")
    override val username: Name? = null,

    @SerialName("language_code")
    override val languageCode: String? = null,

    @SerialName("is_premium")
    override val isPremium: Boolean = false,

    @SerialName("added_to_attachment_menu")
    override val addedToAttachmentMenu: Boolean = false,
) : BaseUser

@Serializable
data class Me(
    @SerialName("id")
    override val id: Id,

    @SerialName("is_bot")
    override val isBot: Boolean,

    @SerialName("first_name")
    override val firstName: String,

    @SerialName("last_name")
    override val lastName: String? = null,

    @SerialName("username")
    override val username: Name? = null,

    @SerialName("language_code")
    override val languageCode: String? = null,

    @SerialName("is_premium")
    override val isPremium: Boolean = false,

    @SerialName("added_to_attachment_menu")
    override val addedToAttachmentMenu: Boolean = false,

    @SerialName("can_join_groups")
    val canJoinGroups: Boolean = false,

    @SerialName("can_read_all_group_messages")
    val canReadAllGroupMessages: Boolean = false,

    @SerialName("supports_inline_queries")
    val supportsInlineQueries: Boolean = false,

    @SerialName("can_connect_to_business")
    val canConnectToBusiness: Boolean = false,

    @SerialName("has_main_web_app")
    val hasMainWebApp: Boolean = false,
) : BaseUser