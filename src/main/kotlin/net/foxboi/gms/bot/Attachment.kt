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


internal const val FILE_ID = "file_id"
internal const val FILE_UNIQUE_ID = "file_unique_id"
internal const val FILE_SIZE = "file_size"
internal const val WIDTH = "width"
internal const val HEIGHT = "height"
internal const val DURATION = "duration"
internal const val THUMBNAIL = "thumbnail"
internal const val FILE_NAME = "file_name"
internal const val MIME_TYPE = "mime_type"

suspend fun Bot.getFile(
    fileId: String
): File {
    return call("getFile") {
        put("file_id", fileId)
    }
}

suspend fun Bot.getFile(
    file: FileAttachment
) = getFile(fileId = file.fileId)

interface Attachment

interface FileAttachment : Attachment {
    val fileId: String
    val fileUniqueId: String
    val fileSize: Long
}

interface NamedFileAttachment : FileAttachment {
    val fileName: String?
    val mimeType: String?
}

interface RectangularAttachment : Attachment {
    val width: Int
    val height: Int
}

interface TemporalAttachment : Attachment {
    val duration: Long
}

interface ThumbnailedAttachment : Attachment {
    val thumbnail: PhotoSize?
}

@Serializable
class File(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,

    @SerialName("file_path")
    val filePath: String? = null,
) : FileAttachment

@Serializable
class PhotoSize(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(WIDTH)
    override val width: Int,

    @SerialName(HEIGHT)
    override val height: Int,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : FileAttachment, RectangularAttachment

@Serializable
class Animation(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(WIDTH)
    override val width: Int,

    @SerialName(HEIGHT)
    override val height: Int,

    @SerialName(DURATION)
    override val duration: Long,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName(FILE_NAME)
    override val fileName: String? = null,

    @SerialName(MIME_TYPE)
    override val mimeType: String? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : NamedFileAttachment, RectangularAttachment, TemporalAttachment, ThumbnailedAttachment

@Serializable // TBD
class Audio(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(DURATION)
    override val duration: Long,

    @SerialName("performer")
    val performer: String? = null,

    @SerialName("title")
    val title: String? = null,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName(FILE_NAME)
    override val fileName: String? = null,

    @SerialName(MIME_TYPE)
    override val mimeType: String? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : NamedFileAttachment, TemporalAttachment, ThumbnailedAttachment

@Serializable // TBD
class Video(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(WIDTH)
    override val width: Int,

    @SerialName(HEIGHT)
    override val height: Int,

    @SerialName(DURATION)
    override val duration: Long,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName("cover")
    val cover: List<PhotoSize> = emptyList(),

    @SerialName("start_timestamp")
    val start: Long = 0L,

    @SerialName(FILE_NAME)
    override val fileName: String? = null,

    @SerialName(MIME_TYPE)
    override val mimeType: String? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : NamedFileAttachment, RectangularAttachment, TemporalAttachment, ThumbnailedAttachment

@Serializable // TBD
class VideoNote(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName("length")
    val sideLength: Int,

    @SerialName(DURATION)
    override val duration: Long,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : FileAttachment, RectangularAttachment, TemporalAttachment, ThumbnailedAttachment {
    override val width: Int
        get() = sideLength

    override val height: Int
        get() = sideLength
}

@Serializable // TBD
class Voice(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(DURATION)
    override val duration: Long,

    @SerialName(MIME_TYPE)
    val mimeType: String? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : FileAttachment, TemporalAttachment

@Serializable // TBD
class Document(
    @SerialName(FILE_ID)
    override val fileId: String,

    @SerialName(FILE_UNIQUE_ID)
    override val fileUniqueId: String,

    @SerialName(THUMBNAIL)
    override val thumbnail: PhotoSize? = null,

    @SerialName(FILE_NAME)
    override val fileName: String? = null,

    @SerialName(MIME_TYPE)
    override val mimeType: String? = null,

    @SerialName(FILE_SIZE)
    override val fileSize: Long = -1,
) : NamedFileAttachment, ThumbnailedAttachment