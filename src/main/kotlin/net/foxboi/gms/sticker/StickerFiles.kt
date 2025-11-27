package net.foxboi.gms.sticker

import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import net.foxboi.gms.bot.flow.Upload

class StickerFiles(
    val root: Path,
    val fs: FileSystem = SystemFileSystem
) {
    fun path(name: String): Path {
        return Path(root, name)
    }

    suspend fun save(name: String, output: suspend (Sink) -> Unit) {
        val path = path(name)

        withContext(Dispatchers.IO) {
            path.parent?.let {
                fs.createDirectories(it, mustCreate = false)
            }

            fs.sink(path).buffered().use {
                output(it)
            }
        }
    }

    suspend fun saveIfNotExists(name: String, output: suspend (Sink) -> Unit) {
        if (!exists(name)) {
            save(name, output)
        }
    }

    suspend fun save(name: String, output: RawSource) {
        save(name) { snk -> snk.transferFrom(output) }
    }

    suspend fun saveIfNotExists(name: String, output: RawSource) {
        saveIfNotExists(name) { snk -> snk.transferFrom(output) }
    }

    suspend fun tryLoad(name: String, input: suspend (Source) -> Unit): Boolean {
        val path = path(name)

        return withContext(Dispatchers.IO) {
            if (fs.exists(path)) {
                fs.source(path).buffered().use {
                    input(it)
                }
                true
            } else {
                false
            }
        }
    }

    suspend fun load(name: String, input: suspend (Source) -> Unit) {
        if (!tryLoad(name, input)) {
            throw FileNotFoundException(name)
        }
    }

    suspend fun tryConvert(fromName: String, toName: String, converter: StickerConverter): Boolean {
        val fromPath = path(fromName)
        val toPath = path(toName)

        return withContext(Dispatchers.IO) {
            if (fs.exists(fromPath)) {
                toPath.parent?.let {
                    fs.createDirectories(it, mustCreate = false)
                }

                fs.source(fromPath).buffered().use { src ->
                    fs.sink(toPath).buffered().use { snk ->
                        converter.convert(src, snk)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    suspend fun tryConvertIfNotExists(fromName: String, toName: String, converter: StickerConverter): Boolean {
        if (!exists(toName)) {
            return tryConvert(fromName, toName, converter)
        }
        return true
    }

    suspend fun convert(fromName: String, toName: String, converter: StickerConverter) {
        if (!tryConvert(fromName, toName, converter)) {
            throw FileNotFoundException(fromName)
        }
    }

    suspend fun convertIfNotExists(fromName: String, toName: String, converter: StickerConverter) {
        if (!tryConvertIfNotExists(fromName, toName, converter)) {
            throw FileNotFoundException(fromName)
        }
    }

    fun uploadOrNull(name: String, contentType: ContentType? = null, rename: String? = null): Upload? {
        val path = path(name)

        if (fs.exists(path)) {
            return Upload(rename ?: path.name, contentType) { snk ->
                fs.source(path).use { src ->
                    snk.transferFrom(src)
                }
            }
        }

        return null
    }

    fun upload(name: String, contentType: ContentType? = null, rename: String? = null): Upload {
        return uploadOrNull(name, contentType, rename) ?: throw FileNotFoundException(name)
    }

    fun exists(name: String): Boolean {
        return fs.exists(path(name))
    }
}