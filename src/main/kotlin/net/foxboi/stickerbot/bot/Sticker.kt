package net.foxboi.stickerbot.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


suspend fun Bot.getStickerSet(
    name: String
): StickerSet {
    return call("getStickerSet") {
        put("name", name)
    }
}

@Serializable // TBD
class Sticker(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName("type")
    val type: StickerType,

    @SerialName(WIDTH)
    override val width: Int,

    @SerialName(HEIGHT)
    override val height: Int,

    @SerialName("is_animated")
    val isAnimated: Boolean,

    @SerialName("is_video")
    val isVideo: Boolean,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName("emoji")
    val emoji: String? = null,

    @SerialName("set_name")
    val setName: String? = null,

    @SerialName("premium_animation")
    val premiumAnimation: File? = null,

    @SerialName("mask_position")
    val maskPosition: MaskPosition? = null,

    @SerialName("custom_emoji_id")
    val customEmojiId: String? = null,

    @SerialName("needs_repainting")
    val needsRepainting: Boolean = false,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : FileAttachment, ThumbnailedAttachment, RectangularAttachment


@Serializable
enum class StickerType {
    @SerialName("regular")
    REGULAR,

    @SerialName("mask")
    MASK,

    @SerialName("custom_emoji")
    CUSTOM_EMOJI
}

@Serializable
enum class MaskPositionPoint {
    @SerialName("forehead")
    FOREHEAD,

    @SerialName("eyes")
    EYES,

    @SerialName("mouth")
    MOUTH,

    @SerialName("chin")
    CHIN
}

@Serializable
class MaskPosition(
    @SerialName("point")
    val point: MaskPositionPoint,

    @SerialName("x_shift")
    val xShift: Double,

    @SerialName("y_shift")
    val yShift: Double,

    @SerialName("scale")
    val scale: Double
)

@Serializable
class StickerSet(
    @SerialName("name")
    val name: String,

    @SerialName("title")
    val title: String,

    @SerialName("sticker_type")
    val stickerType: StickerType,

    @SerialName("stickers")
    val stickers: List<Sticker>,

    @SerialName("thumbnail")
    val thumbnail: PhotoSize? = null,
)


enum class StickerFormat {
    @SerialName("static")
    STATIC,

    @SerialName("animated")
    ANIMATED,

    @SerialName("video")
    VIDEO
}

@Serializable
class InputSticker(
    @SerialName("sticker")
    val sticker: String,

    @SerialName("format")
    val format: StickerFormat,

    @SerialName("emoji_list")
    val emoji: String,

    @SerialName("mask_position")
    val maskPosition: MaskPosition? = null,

    @SerialName("keywords")
    val keywords: List<String> = emptyList(),
)