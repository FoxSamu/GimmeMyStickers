package net.foxboi.gms.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.foxboi.gms.util.DelegateSerializer

@Serializable
private class SerialUpdate(
    @SerialName("update_id")
    val updateId: Long,

    @SerialName("message")
    val message: Message? = null,

    @SerialName("edited_message")
    val editedMessage: Message? = null,

    @SerialName("channel_post")
    val channelPost: Message? = null,

    @SerialName("edited_channel_post")
    val editedChannelPost: Message? = null,

    @SerialName("business_connection")
    val businessConnection: BusinessConnection? = null,

    @SerialName("business_message")
    val businessMessage: Message? = null,

    @SerialName("edited_business_message")
    val editedBusinessMessage: Message? = null,

    @SerialName("deleted_business_messages")
    val deletedBusinessMessages: BusinessMessagesDeleted? = null,

    @SerialName("message_reaction")
    val messageReaction: MessageReactionUpdated? = null,

    @SerialName("message_reaction_count")
    val messageReactionCount: MessageReactionCountUpdated? = null,

    @SerialName("inline_query")
    val inlineQuery: InlineQuery? = null,

    @SerialName("chosen_inline_result")
    val chosenInlineResult: ChosenInlineResult? = null,

    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,

    @SerialName("shipping_query")
    val shippingQuery: ShippingQuery? = null,

    @SerialName("pre_checkout_query")
    val preCheckoutQuery: PreCheckoutQuery? = null,

    @SerialName("purchased_paid_media")
    val purchasedPaidMedia: PaidMediaPurchased? = null,

    @SerialName("poll")
    val poll: Poll? = null,

    @SerialName("poll_answer")
    val pollAnswer: PollAnswer? = null,

    @SerialName("my_chat_member")
    val myChatMember: ChatMemberUpdated? = null,

    @SerialName("chat_member")
    val chatMember: ChatMemberUpdated? = null,

    @SerialName("chat_join_request")
    val chatJoinRequest: ChatJoinRequest? = null,

    @SerialName("chat_boost")
    val chatBoost: ChatBoostUpdated? = null,

    @SerialName("removed_chat_boost")
    val chatBoostRemoved: ChatBoostRemoved? = null,
)

private object UpdateSerializer : DelegateSerializer<Update, SerialUpdate>(SerialUpdate.serializer()) {
    override fun encode(value: Update) = when(value) {
        is MessageUpdate -> SerialUpdate(value.id, message = value.message)
        is EditedMessageUpdate -> SerialUpdate(value.id, editedMessage = value.message)
        is ChannelPostUpdate -> SerialUpdate(value.id, channelPost = value.message)
        is EditedChannelPostUpdate -> SerialUpdate(value.id, editedChannelPost = value.message)
        is BusinessConnectionUpdate -> SerialUpdate(value.id, businessConnection = value.data)
        is BusinessMessageUpdate -> SerialUpdate(value.id, businessMessage = value.message)
        is EditedBusinessMessageUpdate -> SerialUpdate(value.id, editedBusinessMessage = value.message)
        is DeletedBusinessMessagesUpdate -> SerialUpdate(value.id, deletedBusinessMessages = value.data)
        is UnknownUpdate -> SerialUpdate(value.id)
    }

    override fun decode(value: SerialUpdate) = when {
        value.message != null -> MessageUpdate(value.updateId, value.message)
        value.editedMessage != null -> EditedMessageUpdate(value.updateId, value.editedMessage)
        value.channelPost != null -> ChannelPostUpdate(value.updateId, value.channelPost)
        value.editedChannelPost != null -> EditedChannelPostUpdate(value.updateId, value.editedChannelPost)
        value.businessConnection != null -> BusinessConnectionUpdate(value.updateId, value.businessConnection)
        value.businessMessage != null -> BusinessMessageUpdate(value.updateId, value.businessMessage)
        value.editedBusinessMessage != null -> EditedBusinessMessageUpdate(value.updateId, value.editedBusinessMessage)
        value.deletedBusinessMessages != null -> DeletedBusinessMessagesUpdate(value.updateId, value.deletedBusinessMessages)
        else -> UnknownUpdate(value.updateId)
    }
}

@Serializable(UpdateSerializer::class)
sealed interface Update {
    val id: Long
}

class MessageUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("message")
}

class EditedMessageUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("edited_message")
}

class ChannelPostUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("channel_post")
}

class EditedChannelPostUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("edited_channel_post")
}

class BusinessConnectionUpdate internal constructor(
    override val id: Long,
    internal val data: BusinessConnection
) : Update {
    val connectionId by data::connectionId
    val user by data::user
    val userChatId by data::userChatId
    val date by data::date
    val rights by data::rights
    val isEnabled by data::isEnabled

    companion object : UpdateType("business_connection")
}

class BusinessMessageUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("business_message")
}

class EditedBusinessMessageUpdate internal constructor(
    override val id: Long,
    val message: Message
) : Update {
    companion object : UpdateType("edited_business_message")
}

class DeletedBusinessMessagesUpdate internal constructor(
    override val id: Long,
    internal val data: BusinessMessagesDeleted
) : Update {
    val connectionId by data::connectionId
    val chat by data::chat
    val messageIds by data::messageIds

    companion object : UpdateType("deleted_business_messages")
}

data class UnknownUpdate(
    override val id: Long
) : Update



sealed class UpdateType(val name: String) {
    companion object {
        val all = listOf(
            MessageUpdate,
            EditedMessageUpdate,
            ChannelPostUpdate,
            EditedChannelPostUpdate
        )
    }
}


interface UpdateListener {
    suspend fun onUpdate(bot: Bot, update: Update) = when (update) {
        is MessageUpdate -> onMessage(bot, update)
        is EditedMessageUpdate -> onEditedMessage(bot, update)
        is ChannelPostUpdate -> onChannelPost(bot, update)
        is EditedChannelPostUpdate -> onEditedChannelPost(bot, update)
        is BusinessConnectionUpdate -> onBusinessConnection(bot, update)
        is BusinessMessageUpdate -> onBusinessMessage(bot, update)
        is EditedBusinessMessageUpdate -> onEditedBusinessMessage(bot, update)
        is DeletedBusinessMessagesUpdate -> onDeletedBusinessMessage(bot, update)
        is UnknownUpdate -> onUnknownUpdate(bot, update)
    }

    suspend fun onMessage(bot: Bot, update: MessageUpdate) {

    }

    suspend fun onEditedMessage(bot: Bot, update: EditedMessageUpdate) {

    }

    suspend fun onChannelPost(bot: Bot, update: ChannelPostUpdate) {

    }

    suspend fun onEditedChannelPost(bot: Bot, update: EditedChannelPostUpdate) {

    }

    suspend fun onBusinessConnection(bot: Bot, update: BusinessConnectionUpdate) {

    }

    suspend fun onBusinessMessage(bot: Bot, update: BusinessMessageUpdate) {

    }

    suspend fun onEditedBusinessMessage(bot: Bot, update: EditedBusinessMessageUpdate) {

    }

    suspend fun onDeletedBusinessMessage(bot: Bot, update: DeletedBusinessMessagesUpdate) {

    }

    suspend fun onUnknownUpdate(bot: Bot, update: UnknownUpdate) {

    }
}

object UpdateIgnorer : UpdateListener



@Serializable
class BusinessBotRights()

@Serializable
internal class BusinessConnection(
    @SerialName("id")
    val connectionId: String,

    @SerialName("user")
    val user: User,

    @SerialName("userChatId")
    val userChatId: Id,

    @SerialName("date")
    val date: Timestamp,

    @SerialName("rights")
    val rights: BusinessBotRights? = null,

    @SerialName("is_enabled")
    val isEnabled: Boolean = false,
)

@Serializable
internal class BusinessMessagesDeleted(
    @SerialName("business_connection_id")
    val connectionId: String,

    @SerialName("chat")
    val chat: Chat,

    @SerialName("message_ids")
    val messageIds: List<Id>
)

@Serializable
class MessageReactionUpdated()

@Serializable
class MessageReactionCountUpdated()

@Serializable
class InlineQuery()

@Serializable
class ChosenInlineResult()

@Serializable
class CallbackQuery()

@Serializable
class ShippingQuery()

@Serializable
class PreCheckoutQuery()

@Serializable
class PaidMediaPurchased()

@Serializable
class PollAnswer()

@Serializable
class ChatMemberUpdated()

@Serializable
class ChatJoinRequest()

@Serializable
class ChatBoostUpdated()

@Serializable
class ChatBoostRemoved()