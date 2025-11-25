package net.foxboi.stickerbot

import kotlinx.io.files.Path
import net.foxboi.stickerbot.bot.*
import net.foxboi.stickerbot.session.Session
import net.foxboi.stickerbot.storage.FileStorage

object StickerBot : UpdateListener, LifecycleListener, ExceptionHandler {
    val log = Log

    val sessions = FileStorage(
        root = Path(Env.storageDirectory, "./sessions"),
        keyToPath = Session::filePath,
        factory = ::Session,
        maxCached = Env.maxCachedSessions
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

        if (input.trim() == "halt") {
            bot.signalStop(true)
            log.info { "Halting..." }
        }
    }

    override suspend fun onStop(bot: Bot) {
        sessions.close()
        log.info { "Stopped. Bye." }
    }

    override fun onHalt(bot: Bot) {
        sessions.close()
        log.info { "Halted. Bye." }
    }

    override suspend fun onMessage(bot: Bot, update: MessageUpdate) {
        val msg = update.message
        val chat = update.message.chat

        if (msg.isFromBot || msg.from == null) {
            return
        }

        val session = sessions.get(msg.from.id)

        if (msg.text == "/incr") {
            session.increment()

            bot.sendMessage(
                chatId = chat.id,
                text = "Incremented! Your number is now: ${session.number()}",
                directMessagesTopicId = msg.directMessagesTopic?.topicId
            )
        }

        if (msg.text == "/decr") {
            session.decrement()

            bot.sendMessage(
                chatId = chat.id,
                text = "Decremented! Your number is now: ${session.number()}",
                directMessagesTopicId = msg.directMessagesTopic?.topicId
            )
        }

        if (msg.text == "/num") {
            bot.sendMessage(
                chatId = chat.id,
                text = "Your number is: ${session.number()}",
                directMessagesTopicId = msg.directMessagesTopic?.topicId
            )
        }

        session.save()
    }

    override fun onException(bot: Bot, e: Throwable) {
        log.error(e)
    }

    suspend fun run() {
        val bot = Bot(Env.token)
        bot.lifecycleListener = this
        bot.updateListener = this
        bot.exceptionHandler = this
        bot.run()
    }
}
