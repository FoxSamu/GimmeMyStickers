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
import net.foxboi.gms.bot.flow.Upload
import net.foxboi.gms.util.DelegateSerializer


// TODO Allow paid broadcast, Suggested post parameters, Reply markup
suspend fun Bot.sendMessage(
    chatId: IdOrName,
    text: String,
    businessConnectionId: String? = null,
    messageThreadId: Id? = null,
    directMessagesTopicId: Id? = null,
    parseMode: ParseMode? = null,
    entities: List<MessageEntity> = emptyList(),
    linkPreviewOptions: LinkPreviewOptions? = null,
    disableNotification: Boolean? = null,
    protectContent: Boolean? = null,
    messageEffectId: String? = null,
    replyParameters: ReplyParameters? = null,
): Message {
    return call("sendMessage") {
        put("chat_id", chatId)
        put("text", text)
        putMaybe("business_connection_id", businessConnectionId)
        putMaybe("message_thread_id", messageThreadId)
        putMaybe("direct_messages_topic_id", directMessagesTopicId)
        putMaybe("parse_mode", parseMode)
        putMaybe("entities", entities.ifEmpty { null })
        putMaybe("link_preview_options", linkPreviewOptions)
        putMaybe("disable_notification", disableNotification)
        putMaybe("protect_content", protectContent)
        putMaybe("message_effect_id", messageEffectId)
        putMaybe("reply_parameters", replyParameters)
    }
}

suspend fun Bot.sendDocument(
    chatId: IdOrName,
    fileId: String,
    caption: String? = null,
    businessConnectionId: String? = null,
    messageThreadId: Id? = null,
): Message {
    return call("sendDocument") {
        put("chat_id", chatId)
        put("document", fileId)
        putMaybe("caption", caption)
        putMaybe("business_connection_id", businessConnectionId)
        putMaybe("message_thread_id", messageThreadId)
    }
}

suspend fun Bot.sendDocument(
    chatId: IdOrName,
    file: Upload,
    caption: String? = null,
    businessConnectionId: String? = null,
    messageThreadId: Id? = null,
    disableContentTypeDetection: Boolean? = null
): Message {
    return push("sendDocument") {
        put("chat_id", chatId)
        put("document", file)
        putMaybe("caption", caption)
        putMaybe("business_connection_id", businessConnectionId)
        putMaybe("message_thread_id", messageThreadId)
        putMaybe("disable_content_type_detection", disableContentTypeDetection)
    }
}




val Message.isFromBot: Boolean
    get() = from?.isBot ?: false



@Serializable
data class LinkPreviewOptions(
    @SerialName("is_disabled")
    val isDisabled: Boolean? = null,

    @SerialName("url")
    val url: String? = null,

    @SerialName("prefer_small_media")
    val preferSmallMedia: Boolean? = null,

    @SerialName("prefer_large_media")
    val preferLargeMedia: Boolean? = null,

    @SerialName("show_above_text")
    val showAboveText: Boolean? = null,
)

@Serializable
data class MessageId(
    @SerialName("message_id")
    val messageId: Id
)

@Serializable
sealed interface MessageOrigin {
    val date: Timestamp
}

@Serializable
@SerialName("user")
data class MessageOriginUser(
    @SerialName("date")
    override val date: Timestamp,

    @SerialName("sender_user")
    val senderUser: User,
) : MessageOrigin

@Serializable
@SerialName("hidden_user")
data class MessageOriginHiddenUser(
    @SerialName("date")
    override val date: Timestamp,

    @SerialName("sender_user_name")
    val senderUserName: String,
) : MessageOrigin

@Serializable
@SerialName("chat")
data class MessageOriginChat(
    @SerialName("date")
    override val date: Timestamp,

    @SerialName("sender_chat")
    val senderChat: Chat,

    @SerialName("author_signature")
    val authorSignature: String? = null,
) : MessageOrigin

@Serializable
@SerialName("channel")
data class MessageOriginChannel(
    @SerialName("date")
    override val date: Timestamp,

    @SerialName("chat")
    val chat: Chat,

    @SerialName("message_id")
    val messageId: Id,

    @SerialName("author_signature")
    val authorSignature: String? = null,
) : MessageOrigin



@Serializable(MaybeInaccessibleMessage.Serializer::class)
sealed interface MaybeInaccessibleMessage {
    val id: Id
    val chat: Chat
    val date: Timestamp

    // The `date` value being EPOCH indicates an inaccessible message, so delegate serialization via `Message`.
    private object Serializer : DelegateSerializer<MaybeInaccessibleMessage, Message>(Message.serializer()) {
        override fun encode(value: MaybeInaccessibleMessage): Message {
            if (value is Message) {
                return value
            }

            return Message(
                id = value.id,
                chat = value.chat,
                date = value.date
            )
        }

        override fun decode(value: Message): MaybeInaccessibleMessage {
            if (value.date.isEpoch()) {
                return InaccessibleMessage(
                    id = value.id,
                    chat = value.chat,
                    date = value.date
                )
            }

            return value
        }
    }
}

@Serializable
data class InaccessibleMessage(
    @SerialName("message_id")
    override val id: Id,

    @SerialName("chat")
    override val chat: Chat,

    @SerialName("date")
    override val date: Timestamp
) : MaybeInaccessibleMessage

@Serializable
data class Message(
    @SerialName("message_id")
    override val id: Id,

    @SerialName("chat")
    override val chat: Chat,

    @SerialName("date")
    override val date: Timestamp,

    @SerialName("message_thread_id")
    val threadId: Id? = null,

    @SerialName("direct_messages_topic")
    val directMessagesTopic: DirectMessagesTopic? = null,

    @SerialName("from")
    val from: User? = null,

    @SerialName("sender_chat")
    val senderChat: Chat? = null,

    @SerialName("sender_boost_count")
    val senderBoostCount: Int = 0,

    @SerialName("sender_business_bot")
    val senderBusinessBot: User? = null,

    @SerialName("business_connection_id")
    val businessConnectionId: String? = null,

    @SerialName("forward_origin")
    val forwardOrigin: MessageOrigin? = null,

    @SerialName("is_topic_message")
    val isTopicMessage: Boolean = false,

    @SerialName("is_automatic_forward")
    val isAutomaticForward: Boolean = false,

    @SerialName("reply_to_message")
    val replyToMessage: Message? = null,

    @SerialName("external_reply")
    val externalReply: ExternalReplyInfo? = null,

    @SerialName("quote")
    val quote: TextQuote? = null,

    @SerialName("reply_to_story")
    val replyToStory: Story? = null,

    @SerialName("reply_to_checklist_task_id")
    val replyToChecklistTaskId: Id? = null,

    @SerialName("via_bot")
    val viaBot: User? = null,

    @SerialName("edit_date")
    val editDate: Timestamp? = null,

    @SerialName("has_protected_content")
    val hasProtectedContent: Boolean = false,

    @SerialName("is_from_offline")
    val isFromOffline: Boolean = false,

    @SerialName("is_paid_post")
    val isPaidPost: Boolean = false,

    @SerialName("media_group_id")
    val mediaGroupId: String? = null,

    @SerialName("author_signature")
    val authorSignature: String? = null,

    @SerialName("paid_star_count")
    val paidStarCount: Int = 0,

    @SerialName("text")
    val text: String? = null,

    @SerialName("entities")
    val entities: List<MessageEntity> = emptyList(),

    @SerialName("link_preview_options")
    val linkPreviewOptions: LinkPreviewOptions? = null,

    @SerialName("suggested_post_info")
    val suggestedPostInfo: SuggestedPostInfo? = null,

    @SerialName("effect_id")
    val effectId: String? = null,

    @SerialName("animation")
    val animation: Animation? = null,

    @SerialName("audio")
    val audio: Audio? = null,

    @SerialName("document")
    val document: Document? = null,

    @SerialName("paid_media")
    val paidMedia: PaidMediaInfo? = null,

    @SerialName("photo")
    val photo: List<PhotoSize> = emptyList(),

    @SerialName("sticker")
    val sticker: Sticker? = null,

    @SerialName("story")
    val story: Story? = null,

    @SerialName("video")
    val video: Video? = null,

    @SerialName("video_note")
    val videoNote: VideoNote? = null,

    @SerialName("voice")
    val voice: Voice? = null,

    @SerialName("caption")
    val caption: String? = null,

    @SerialName("caption_entities")
    val captionEntities: List<MessageEntity> = emptyList(),

    @SerialName("show_caption_above_media")
    val showCaptionAboveMedia: Boolean = false,

    @SerialName("has_media_spoiler")
    val hasMediaSpoiler: Boolean = false,

    @SerialName("checklist")
    val checklist: Checklist? = null,

    @SerialName("contact")
    val contact: Contact? = null,

    @SerialName("game")
    val game: Game? = null,

    @SerialName("poll")
    val poll: Poll? = null,

    @SerialName("venue")
    val venue: Venue? = null,

    @SerialName("location")
    val location: Location? = null,

    @SerialName("new_chat_members")
    val newChatMembers: List<User> = emptyList(),

    @SerialName("left_chat_member")
    val leftChatMember: User? = null,

    @SerialName("new_chat_title")
    val newChatTitle: String? = null,

    @SerialName("new_chat_photo")
    val newChatPhoto: List<PhotoSize> = emptyList(),

    @SerialName("delete_chat_photo")
    val deleteChatPhoto: Boolean = false,

    @SerialName("group_chat_created")
    val groupChatCreated: Boolean = false,

    @SerialName("supergroup_chat_created")
    val supergroupChatCreated: Boolean = false,

    @SerialName("channel_chat_created")
    val channelChatCreated: Boolean = false,

    @SerialName("message_auto_delete_timer_changed")
    val messageAutoDeleteTimerChanged: MessageAutoDeleteTimerChanged? = null,

    @SerialName("migrate_to_chat_id")
    val migrateToChatId: Id? = null,

    @SerialName("migrate_from_chat_id")
    val migrateFromChatId: Id? = null,

    @SerialName("pinned_message")
    val pinnedMessage: MaybeInaccessibleMessage? = null,

    @SerialName("invoice")
    val invoice: Invoice? = null,

    @SerialName("successful_payment")
    val successfulPayment: SuccessfulPayment? = null,

    @SerialName("refunded_payment")
    val refundedPayment: RefundedPayment? = null,

    @SerialName("users_shared")
    val usersShared: UsersShared? = null,

    @SerialName("chat_shared")
    val chatShared: ChatShared? = null,

    @SerialName("gift")
    val gift: GiftInfo? = null,

    @SerialName("unique_gift")
    val uniqueGift: UniqueGiftInfo? = null,

    @SerialName("connected_website")
    val connectedWebsite: String? = null,

    @SerialName("write_access_allowed")
    val writeAccessAllowed: WriteAccessAllowed? = null,

    @SerialName("passport_data")
    val passportData: PassportData? = null,

    @SerialName("proximity_alert_triggered")
    val proximityAlertTriggered: ProximityAlertTriggered? = null,

    @SerialName("boost_added")
    val boostAdded: ChatBoostAdded? = null,

    @SerialName("chat_background_set")
    val chatBackgroundSet: ChatBackground? = null,

    @SerialName("checklist_tasks_done")
    val checklistTasksDone: ChecklistTasksDone? = null,

    @SerialName("checklist_tasks_added")
    val checklistTasksAdded: ChecklistTasksAdded? = null,

    @SerialName("direct_message_price_changed")
    val directMessagePriceChanged: DirectMessagePriceChanged? = null,

    @SerialName("forum_topic_created")
    val forumTopicCreated: ForumTopicCreated? = null,

    @SerialName("forum_topic_edited")
    val forumTopicEdited: ForumTopicEdited? = null,

    @SerialName("forum_topic_closed")
    val forumTopicClosed: ForumTopicClosed? = null,

    @SerialName("forum_topic_reopened")
    val forumTopicReopened: ForumTopicReopened? = null,

    @SerialName("general_forum_topic_hidden")
    val generalForumTopicHidden: GeneralForumTopicHidden? = null,

    @SerialName("general_forum_topic_unhidden")
    val generalForumTopicUnhidden: GeneralForumTopicUnhidden? = null,

    @SerialName("giveaway_created")
    val giveawayCreated: GiveawayCreated? = null,

    @SerialName("giveaway")
    val giveaway: Giveaway? = null,

    @SerialName("giveaway_winners")
    val giveawayWinners: GiveawayWinners? = null,

    @SerialName("giveaway_completed")
    val giveawayCompleted: GiveawayCompleted? = null,

    @SerialName("paid_message_price_changed")
    val paidMessagePriceChanged: PaidMessagePriceChanged? = null,

    @SerialName("suggested_post_approved")
    val suggestedPostApproved: SuggestedPostApproved? = null,

    @SerialName("suggested_post_approval_failed")
    val suggestedPostApprovalFailed: SuggestedPostApprovalFailed? = null,

    @SerialName("suggested_post_declined")
    val suggestedPostDeclined: SuggestedPostDeclined? = null,

    @SerialName("suggested_post_paid")
    val suggestedPostPaid: SuggestedPostPaid? = null,

    @SerialName("suggested_post_refunded")
    val suggestedPostRefunded: SuggestedPostRefunded? = null,

    @SerialName("video_chat_scheduled")
    val videoChatScheduled: VideoChatScheduled? = null,

    @SerialName("video_chat_started")
    val videoChatStarted: VideoChatStarted? = null,

    @SerialName("video_chat_ended")
    val videoChatEnded: VideoChatEnded? = null,

    @SerialName("video_chat_participants_invited")
    val videoChatParticipantsInvited: VideoChatParticipantsInvited? = null,

    @SerialName("web_app_data")
    val webAppData: WebAppData? = null,

    @SerialName("reply_markup")
    val replyMarkup: InlineKeyboardMarkup? = null,
) : MaybeInaccessibleMessage


@Serializable
data class MessageEntity(
    @SerialName("type")
    val type: MessageEntityType,

    @SerialName("offset")
    val offset: Int,

    @SerialName("length")
    val length: Int,

    @SerialName("url")
    val url: String? = null,

    @SerialName("user")
    val user: User? = null,

    @SerialName("language")
    val language: String? = null,

    @SerialName("custom_emoji_id")
    val customEmojiId: String? = null,
)

@Serializable
@Suppress("unused")
enum class MessageEntityType {
    @SerialName("mention")
    MENTION,

    @SerialName("hashtag")
    HASHTAG,

    @SerialName("cashtag")
    CASHTAG,

    @SerialName("bot_command")
    BOT_COMMAND,

    @SerialName("url")
    URL,

    @SerialName("email")
    EMAIL,

    @SerialName("phone_number")
    PHONE_NUMBER,

    @SerialName("bold")
    BOLD,

    @SerialName("italic")
    ITALIC,

    @SerialName("underline")
    UNDERLINE,

    @SerialName("strikethrough")
    STRIKETHROUGH,

    @SerialName("spoiler")
    SPOILER,

    @SerialName("blockquote")
    BLOCKQUOTE,

    @SerialName("expandable_blockquote")
    EXPANDABLE_BLOCKQUOTE,

    @SerialName("code")
    CODE,

    @SerialName("pre")
    PRE,

    @SerialName("text_link")
    TEXT_LINK,

    @SerialName("text_mention")
    TEXT_MENTION,

    @SerialName("custom_emoji")
    CUSTOM_EMOJI,
}

@Serializable
data class TextQuote(
    @SerialName("text")
    val text: String,

    @SerialName("entities")
    val entities: List<MessageEntity> = emptyList(),

    @SerialName("position")
    val position: Int,

    @SerialName("is_manual")
    val isManual: Boolean = false,
)


@Serializable
data class ExternalReplyInfo(
    @SerialName("origin")
    val origin: MessageOrigin,

    @SerialName("chat")
    val chat: Chat,

    @SerialName("message_id")
    val messageId: Id? = null,

    @SerialName("link_preview_options")
    val linkPreviewOptions: LinkPreviewOptions? = null,

    @SerialName("animation")
    val animation: Animation? = null,

    @SerialName("audio")
    val audio: Audio? = null,

    @SerialName("document")
    val document: Document? = null,

    @SerialName("paid_media")
    val paidMedia: PaidMediaInfo? = null,

    @SerialName("photo")
    val photo: List<PhotoSize> = emptyList(),

    @SerialName("sticker")
    val sticker: Sticker? = null,

    @SerialName("story")
    val story: Story? = null,

    @SerialName("video")
    val video: Video? = null,

    @SerialName("video_note")
    val videoNote: VideoNote? = null,

    @SerialName("voice")
    val voice: Voice? = null,

    @SerialName("has_media_spoiler")
    val hasMediaSpoiler: Boolean = false,

    @SerialName("checklist")
    val checklist: Checklist? = null,

    @SerialName("contact")
    val contact: Contact? = null,

    @SerialName("dice")
    val dice: Dice? = null,

    @SerialName("game")
    val game: Game? = null,

    @SerialName("giveaway")
    val giveaway: Giveaway? = null,

    @SerialName("giveaway_winners")
    val giveawayWinners: GiveawayWinners? = null,

    @SerialName("invoice")
    val invoice: Invoice? = null,

    @SerialName("location")
    val location: Location? = null,

    @SerialName("poll")
    val poll: Poll? = null,

    @SerialName("venue")
    val venue: Venue? = null,
)

@Serializable
data class ReplyParameters(
    @SerialName("message_id")
    val messageId: Id,

    @SerialName("chat_id")
    val chatId: IdOrName? = null,

    @SerialName("allow_sending_without_reply")
    val allowSendingWithoutReply: Boolean? = null,

    @SerialName("quote")
    val quote: String? = null,

    @SerialName("quote_parse_mode")
    val quoteParseMode: ParseMode? = null,

    @SerialName("quote_entities")
    val quoteEntities: List<MessageEntity> = emptyList(),

    @SerialName("quote_position")
    val quotePosition: Int? = null,

    @SerialName("checklist_task_id")
    val checklistTaskId: Id? = null,
)

@Serializable
enum class ParseMode {
    @SerialName("MarkdownV2")
    MARKDOWN_V2,

    @SerialName("HTML")
    HTML,

    @SerialName("Markdown")
    MARKDOWN
}

@Serializable // TBD
class Story {

}

@Serializable // TBD
class SuggestedPostInfo {

}

@Serializable // TBD
class PaidMediaInfo {

}

@Serializable // TBD
class Checklist {

}

@Serializable // TBD
class Contact {

}

@Serializable // TBD
class Dice {

}

@Serializable // TBD
class Game {

}

@Serializable // TBD
class Poll {

}

@Serializable // TBD
class Venue {

}

@Serializable // TBD
class Location {

}

@Serializable // TBD
class MessageAutoDeleteTimerChanged {

}

@Serializable // TBD
class Invoice {

}

@Serializable // TBD
class SuccessfulPayment {

}

@Serializable // TBD
class RefundedPayment {

}

@Serializable // TBD
class UsersShared {

}

@Serializable // TBD
class ChatShared {

}

@Serializable // TBD
class GiftInfo {

}

@Serializable // TBD
class UniqueGiftInfo {

}

@Serializable // TBD
class WriteAccessAllowed {

}

@Serializable // TBD
class PassportData {

}


@Serializable
class ProximityAlertTriggered {

}

@Serializable
class ChatBoostAdded {

}

@Serializable
class ChatBackground {

}

@Serializable
class ChecklistTasksDone {

}

@Serializable
class ChecklistTasksAdded {

}

@Serializable
class DirectMessagePriceChanged {

}

@Serializable
class ForumTopicCreated {

}

@Serializable
class ForumTopicEdited {

}

@Serializable
class ForumTopicClosed {

}

@Serializable
class ForumTopicReopened {

}

@Serializable
class GeneralForumTopicHidden {

}

@Serializable
class GeneralForumTopicUnhidden {

}

@Serializable
class GiveawayCreated {

}

@Serializable
class Giveaway {

}

@Serializable
class GiveawayWinners {

}

@Serializable
class GiveawayCompleted {

}

@Serializable
class PaidMessagePriceChanged {

}

@Serializable
class SuggestedPostApproved {

}

@Serializable
class SuggestedPostApprovalFailed {

}

@Serializable
class SuggestedPostDeclined {

}

@Serializable
class SuggestedPostPaid {

}

@Serializable
class SuggestedPostRefunded {

}

@Serializable
class VideoChatScheduled {

}

@Serializable
class VideoChatStarted {

}

@Serializable
class VideoChatEnded {

}

@Serializable
class VideoChatParticipantsInvited {

}

@Serializable
class WebAppData {

}

@Serializable
class InlineKeyboardMarkup {

}