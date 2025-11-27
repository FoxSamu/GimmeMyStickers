package net.foxboi.gms.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface BaseChat {
    val id: Id
    val type: ChatType
    val title: String?
    val username: Name?
    val firstName: String?
    val lastName: String?
    val isForum: Boolean
    val isDirectMessages: Boolean
}

@Serializable
data class Chat(
    @SerialName("id")
    override val id: Id,

    @SerialName("type")
    override val type: ChatType,

    @SerialName("title")
    override val title: String? = null,

    @SerialName("username")
    override val username: Name? = null,

    @SerialName("first_name")
    override val firstName: String? = null,

    @SerialName("last_name")
    override val lastName: String? = null,

    @SerialName("is_forum")
    override val isForum: Boolean = false,

    @SerialName("is_direct_messages")
    override val isDirectMessages: Boolean = false,
) : BaseChat

@Serializable
data class ChatFullInfo(
    @SerialName("id")
    override val id: Id,

    @SerialName("type")
    override val type: ChatType,

    @SerialName("title")
    override val title: String? = null,

    @SerialName("username")
    override val username: Name? = null,

    @SerialName("first_name")
    override val firstName: String? = null,

    @SerialName("last_name")
    override val lastName: String? = null,

    @SerialName("is_forum")
    override val isForum: Boolean = false,

    @SerialName("is_direct_messages")
    override val isDirectMessages: Boolean = false,

    @SerialName("accent_color_id")
    val accentColorId: Int,

    @SerialName("max_reaction_count")
    val maxReactionCount: Int,

    @SerialName("photo")
    val photo: ChatPhoto? = null,

    @SerialName("active_usernames")
    val activeUsernames: List<String> = emptyList(),

    @SerialName("birthdate")
    val birthdate: Birthdate? = null,

    @SerialName("business_intro")
    val businessIntro: BusinessIntro? = null,

    @SerialName("business_location")
    val businessLocation: BusinessLocation? = null,

    @SerialName("business_opening_hours")
    val businessOpeningHours: BusinessOpeningHours? = null,

    @SerialName("personal_chat")
    val personalChat: Chat? = null,

    @SerialName("parent_chat")
    val parentChat: Chat? = null,

    @SerialName("available_reactions")
    val availableReactions: List<ReactionType>? = null,

    @SerialName("background_custom_emoji_id")
    val backgroundCustomEmojiId: String? = null,

    @SerialName("profile_accent_color_id")
    val profileAccentColorId: Int? = null,

    @SerialName("profile_background_custom_emoji_id")
    val profileBackgroundCustomEmojiId: String? = null,

    @SerialName("emoji_status_custom_emoji_id")
    val emojiStatusCustomEmojiId: String? = null,

    @SerialName("emoji_status_expiration_date")
    val emojiStatusExpirationDate: Long? = null,

    @SerialName("bio")
    val bio: String? = null,

    @SerialName("has_private_forwards")
    val hasPrivateForwards: Boolean = false,

    @SerialName("has_restricted_voice_and_video_messages")
    val hasRestrictedVoiceAndVideoMessages: Boolean = false,

    @SerialName("join_to_send_messages")
    val joinToSendMessages: Boolean = false,

    @SerialName("join_by_request")
    val joinByRequest: Boolean = false,

    @SerialName("description")
    val description: String? = null,

    @SerialName("invite_link")
    val inviteLink: String? = null,

    @SerialName("pinned_message")
    val pinnedMessage: Message? = null,

    @SerialName("permissions")
    val permissions: ChatPermissions? = null,

    @SerialName("accepted_gift_types")
    val acceptedGiftTypes: AcceptedGiftTypes,

    @SerialName("can_send_paid_media")
    val canSendPaidMedia: Boolean = false,

    @SerialName("slow_mode_delay")
    val slowModeDelay: Long = 0L,

    @SerialName("unrestrict_boost_count")
    val unrestrictBoostCount: Int? = null,

    @SerialName("message_auto_delete_time")
    val messageAutoDeleteTime: Long? = null,

    @SerialName("has_aggressive_anti_spam_enabled")
    val hasAggressiveAntiSpamEnabled: Boolean = false,

    @SerialName("has_hidden_members")
    val hasHiddenMembers: Boolean = false,

    @SerialName("has_protected_content")
    val hasProtectedContent: Boolean = false,

    @SerialName("has_visible_history")
    val hasVisibleHistory: Boolean = false,

    @SerialName("sticker_set_name")
    val stickerSetName: String? = null,

    @SerialName("can_set_sticker_set")
    val canSetStickerSet: Boolean = false,

    @SerialName("custom_emoji_sticker_set_name")
    val customEmojiStickerSetName: String? = null,

    @SerialName("linked_chat_id")
    val linkedChatId: Id? = null,

    @SerialName("location")
    val location: ChatLocation? = null,
) : BaseChat

@Serializable
enum class ChatType {
    @SerialName("private")
    PRIVATE,

    @SerialName("group")
    GROUP,

    @SerialName("supergroup")
    SUPERGROUP,

    @SerialName("channel")
    CHANNEL
}

@Serializable
data class DirectMessagesTopic(
    @SerialName("topic_id")
    val topicId: Id,

    @SerialName("user")
    val user: User? = null
)

@Serializable // TBD
class ChatPhoto()

@Serializable // TBD
class Birthdate()

@Serializable // TBD
class BusinessIntro()

@Serializable // TBD
class BusinessLocation()

@Serializable // TBD
class BusinessOpeningHours()

@Serializable // TBD
class ReactionType()

@Serializable // TBD
class ChatPermissions()

@Serializable // TBD
class AcceptedGiftTypes()

@Serializable // TBD
class ChatLocation()