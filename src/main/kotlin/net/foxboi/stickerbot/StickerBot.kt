package net.foxboi.stickerbot

import io.ktor.http.*
import kotlinx.io.files.Path
import net.foxboi.stickerbot.bot.*
import net.foxboi.stickerbot.sticker.*
import net.foxboi.stickerbot.sticker.StickerFormat
import net.foxboi.stickerbot.storage.FileStorage
import net.foxboi.stickerbot.util.TgSticker
import java.security.MessageDigest
import kotlin.io.encoding.Base64

object StickerBot : UpdateListener, LifecycleListener, ExceptionHandler {
    val log = Log

    val sessions = FileStorage<Id, Session>(
        root = Path(Env.storageDirectory, "./sessions"),
        keyToPath = { id -> hashUserId(id) },
        factory = { _, loc -> Session(loc) },
        maxCached = Env.maxCachedSessions
    )

    val stickers = StickerFiles(
        root = Path(Env.storageDirectory, "./stickers")
    )

    override suspend fun onReady(bot: Bot) {
        val me = bot.getMe()

        log.info { "Connected to bot ${me.username}" }
    }

    override suspend fun onOccasion(bot: Bot) {
        sessions.flush()
    }

    override suspend fun onInput(bot: Bot, input: String) {
        if (input.trim() == "stop") {
            bot.signalStop()
            log.info { "Stopping..." }
        }
    }

    override suspend fun onStop(bot: Bot) {
        sessions.close()
        log.info { "Stopped. Bye." }
    }

    override suspend fun onMessage(bot: Bot, update: MessageUpdate) {
        val message = update.message

        if (message.isFromBot || message.from == null) {
            return
        }

        val context = MessageContext(
            bot = bot,
            session = sessions.get(message.from.id),
            chat = message.chat,
            message = message
        )

        when {
            message.sticker != null -> context.handleStickerMessage(message.sticker)
            message.text != null -> context.handleTextMessage(message.text)
        }

        context.session.save()
    }

    private fun hashUserId(id: Id): String {
        val sha = MessageDigest.getInstance("SHA256").digest(id.bytes.toByteArray())
        return Base64.UrlSafe.encode(sha)
    }

    override fun onException(bot: Bot, e: Throwable) {
        e.printStackTrace()
        log.error(e)
    }

    suspend fun run() {
        log.info { "Running GMS v${BuildInfo.VERSION}" }

        val bot = Bot(Env.token)
        bot.lifecycleListener = this
        bot.updateListener = this
        bot.exceptionHandler = this
        bot.readStandardInput = true
        bot.run()
    }


    private suspend fun MessageContext.handleStickerMessage(sticker: Sticker) {
        val file = bot.getFile(sticker)
        if (file.filePath == null) {
            return
        }

        when (sticker.format) {
            StickerFormat.WEBP -> handleWebp(file, file.filePath)
            StickerFormat.TGS -> handleTgs(file, file.filePath)
            StickerFormat.WEBM -> handleWebm(file, file.filePath)
        }
    }

    private suspend fun MessageContext.handleTextMessage(text: String) {
        when (text) {
            "/png" -> preference { it.wants(png = true) }
            "/jpeg" -> preference { it.wants(jpeg = true) }
            "/webp" -> preference { it.wants(webp = true) }
            "/bmp" -> preference { it.wants(bmp = true) }
            "/all" -> preference { it.wants(png = true, jpeg = true, webp = true, bmp = true) }
            "/help" -> help()
            "/start" -> start()
        }
    }

    private suspend fun MessageContext.handleWebp(file: File, filePath: String) {
        stickers.saveIfNotExists("${file.fileUniqueId}.webp") { snk ->
            snk.transferFrom(bot.pull(filePath))
        }

        if (!session.wantsPng() && !session.wantsJpeg() && !session.wantsBmp() && !session.wantsWebp()) {
            session.wants(png = true)
        }

        val filename = session.fileName()

        if (session.wantsPng()) {
            convertAndSend(file.fileUniqueId, ".png", ContentType.Image.PNG, filename, ToPng)
        }

        if (session.wantsJpeg()) {
            convertAndSend(file.fileUniqueId, ".jpeg", ContentType.Image.JPEG, filename, ToJpeg)
        }

        if (session.wantsBmp()) {
            convertAndSend(file.fileUniqueId, ".bmp", ContentType.Image.BMP, filename, ToJpeg)
        }

        if (session.wantsWebp()) {
            convertAndSend(file.fileUniqueId, ".webp", ContentType.Image.WEBP, filename, null)
        }

        session.incrementFileCounter()
    }

    private suspend fun MessageContext.handleTgs(file: File, filePath: String) {
        stickers.saveIfNotExists("${file.fileUniqueId}.tgs") { snk ->
            snk.transferFrom(bot.pull(filePath))
        }

        stickers.convertIfNotExists("${file.fileUniqueId}.tgs", "${file.fileUniqueId}.json", GUnzip)

        val filename = session.fileName()
        val upload = stickers.upload(
            name = "${file.fileUniqueId}.json",
            contentType = ContentType.Application.GZip,
            rename = "$filename.json"
        )

        bot.sendDocument(
            chatId = chat.id,
            file = upload,
            disableContentTypeDetection = true,
            caption = "I don't support converting animated stickers. Here is your sticker as a Lottie animation."
        )

        session.incrementFileCounter()
    }

    private suspend fun MessageContext.handleWebm(file: File, filePath: String) {
        stickers.saveIfNotExists("${file.fileUniqueId}.webm") { snk ->
            snk.transferFrom(bot.pull(filePath))
        }

        val filename = session.fileName()
        val upload = stickers.upload(
            name = "${file.fileUniqueId}.webm",
            contentType = ContentType.Application.TgSticker,
            rename = "$filename.webm"
        )

        bot.sendDocument(
            chatId = chat.id,
            file = upload,
            disableContentTypeDetection = true,
            caption = "I don't support converting video stickers. Here is your sticker as a WEBM."
        )

        session.incrementFileCounter()
    }

    private suspend fun MessageContext.start() {
        bot.sendMessage(
            chat.id,
            """
                Hi! Welcome to the GimmeMyStickers bot. Send me a sticker and I will send it back as a file. Send me /help for more info.
            """.trimIndent()
        )
    }

    private suspend fun MessageContext.help() {
        val pref = mutableListOf<String>()
        if (session.wantsPng()) {
            pref += "PNG"
        }
        if (session.wantsJpeg()) {
            pref += "JPEG"
        }
        if (session.wantsBmp()) {
            pref += "BMP"
        }
        if (session.wantsWebp()) {
            pref += "WEBP"
        }

        bot.sendMessage(
            chat.id,
            """
                Hi! Welcome to the GimmeMyStickers bot. Send me a sticker and I will send it back as a file.
                Currently, you will receive your stickers as: ${pref.joinToString()}.
                
                **The following commands are available:**
                /png: Get subsequent stickers stickers as PNG.
                /jpeg: Get subsequent stickers stickers as JPEG.
                /bmp: Get subsequent stickers stickers as BMP.
                /webp: Get subsequent stickers stickers as WEBP.
                /all: Get subsequent stickers stickers in every format.
                /help: Receive this help message.
                
                **About:**
                GimmeMyStickers is a free and open source Telegram bot that converts stickers to files so you can download them. It does not send advertismenets or any other unwanted messages and has no paid features.
            """.trimIndent()
        )
    }

    private suspend fun MessageContext.preference(config: (Session) -> Unit) {
        config(session)
        bot.sendMessage(chat.id, "Your preferences have been updated.")
    }

    private suspend fun MessageContext.convertAndSend(
        fileId: String,
        extension: String,
        mime: ContentType,
        filename: String,
        conv: StickerConverter?
    ) {
        if (conv != null) {
            stickers.convertIfNotExists("${fileId}.webp", "$fileId$extension", conv)
        }

        val upload = stickers.upload("$fileId$extension", mime, "$filename$extension")

        bot.sendDocument(
            chatId = chat.id,
            file = upload,
            disableContentTypeDetection = true
        )
    }
}
