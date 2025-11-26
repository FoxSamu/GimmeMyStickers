package net.foxboi.stickerbot.bot

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import net.foxboi.stickerbot.Now
import net.foxboi.stickerbot.util.Condition
import net.foxboi.stickerbot.util.Future
import net.foxboi.stickerbot.util.setDone
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class Bot(
    private val token: String
) {
    var updateListener: UpdateListener = UpdateIgnorer
    var lifecycleListener: LifecycleListener = LifecycleIgnorer
    var exceptionHandler: ExceptionHandler = ExceptionPrinter

    var pollInterval: Long = 10L
    var occasionInterval: Long = 3L

    private var allowedUpdates: Set<UpdateType>? = setOf()
    private var pauseUpdates = false
    private val pauseUpdatesCondition = Condition()

    private val initMutex = Mutex()
    private var running = false
    private var stop = false

    private var ready = Future<Unit>()

    private var lastOccasion = -1L

    private lateinit var pollLoop: Future<Job>
    private lateinit var occasionLoop: Future<Job>
    private lateinit var inputLoop: Future<Job>
    private lateinit var input: Input

    private lateinit var scope: CoroutineScope

    val json = Json {
        ignoreUnknownKeys = true
    }

    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private suspend fun <T> handleResponse(response: HttpResponse, deserializer: DeserializationStrategy<T>): T {
        if (!response.status.isSuccess()) {
            throw HttpException(response.status.value, response.status.description)
        }

        val result = response.body<SerialResult>()

        if (!result.ok) {
            throw TelegramException(result.errorCode, result.description)
        }

        return json.decodeFromJsonElement(deserializer, result.result!!)
    }

    @Serializable
    private class SerialResult(
        @SerialName("ok")
        val ok: Boolean,

        @SerialName("description")
        val description: String? = null,

        @SerialName("error_code")
        val errorCode: Int = 0,

        @SerialName("result")
        val result: JsonElement? = null,

        @SerialName("parameters")
        val parameters: JsonElement? = null,
    )

    suspend fun <R> pull(
        filePath: String,
        handler: (Source) -> R
    ): R {
        if (!ready.isSet()) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.get("https://api.telegram.org/file/bot$token/$filePath")

        if (!response.status.isSuccess()) {
            throw HttpException(response.status.value, response.status.description)
        }

        return withContext(Dispatchers.IO) {
            response.bodyAsChannel().asSource().buffered().use {
                handler(it)
            }
        }
    }

    suspend fun <R> push(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        push: PushBuilder.() -> Unit
    ): R {
        if (!ready.isSet()) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.post("https://api.telegram.org/bot$token/$endpoint") {
            setBody(
                MultiPartFormDataContent(
                    withContext(Dispatchers.IO) {
                        formData {
                            PushBuilder(this, json).push()
                        }
                    }
                )
            )
        }
        return handleResponse(response, resultDeserializer)
    }

    suspend inline fun <reified R> push(
        endpoint: String,
        noinline push: PushBuilder.() -> Unit
    ): R {
        return push(
            endpoint,
            json.serializersModule.serializer(),
            push
        )
    }

    suspend fun <R> call(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        input: JsonObject
    ): R {
        if (!ready.isSet()) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.post("https://api.telegram.org/bot$token/$endpoint") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(input))
        }
        return handleResponse(response, resultDeserializer)
    }

    suspend inline fun <R> call(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        input: QueryBuilder.() -> Unit = {}
    ): R {
        return call(endpoint, resultDeserializer, buildJsonObject {
            input(QueryBuilder(this, json))
        })
    }

    suspend inline fun <reified R> call(
        endpoint: String,
        input: JsonObject
    ): R {
        return call(
            endpoint,
            json.serializersModule.serializer(),
            input
        )
    }

    suspend inline fun <reified R> call(
        endpoint: String,
        input: QueryBuilder.() -> Unit = {}
    ): R {
        return call(
            endpoint,
            json.serializersModule.serializer(),
            input
        )
    }


    /**
     * Allow the bot to receive only the given updates types. Each [Update] implementation except for [UnknownUpdate] has a
     * companion object that represents the [UpdateType] belonging to that [Update] implementation.
     *
     * The set of allowed updates will be supplied to every update poll that follows, until the set of updates is updated
     * again.
     *
     * Example usage:
     * ```kotlin
     * // Instructs the bot to only request MessageUpdate and EditMessageUpdate
     * bot.allowUpdates(MessageUpdate, EditMessageUpdate)
     * ```
     *
     * Note that when no update types are given, then no updates will be received.
     */
    fun allowUpdates(vararg types: UpdateType) {
        allowedUpdates = setOf(*types)
    }

    /**
     * Allow the bot to receive only the given updates types. Each [Update] implementation except for [UnknownUpdate] has a
     * companion object that represents the [UpdateType] belonging to that [Update] implementation.
     *
     * The set of allowed updates will be supplied to every update poll that follows, until the set of updates is updated
     * again.
     *
     * Note that when the given iterable is empty, then no updates will be received.
     */
    fun allowUpdates(types: Iterable<UpdateType>) {
        allowedUpdates = types.toSet()
    }

    /**
     * Allow the bot to receive all updates types. Some updates are by default excluded by Telegram unless explicitly requested.
     * These updates usually require specific admin permissions to be enabled as well via BotFather. This method makes sure to
     * explicitly request even the update types that telegram doesn't send by default.
     */
    fun allowAllUpdates() {
        allowUpdates(UpdateType.all)
    }

    /**
     * Allow the bot to receive the default updates types. Some updates are by default excluded by Telegram unless explicitly requested.
     * These updates usually require specific admin permissions to be enabled as well via BotFather. This method sets the bot to request
     * only the updates that Telegram sends by default.
     */
    fun allowDefaultUpdates() {
        allowedUpdates = null
    }

    fun pauseUpdates() {
        pauseUpdates = true
    }

    fun resumeUpdates() {
        pauseUpdates = false

        // Signalling is a suspending action
        scope.launch {
            pauseUpdatesCondition.signal()
        }
    }


    private inline fun doGuarded(action: () -> Unit) {
        doGuarded(Unit, action)
    }

    private inline fun <T> doGuarded(fallback: T, action: () -> T): T {
        return try {
            action()
        } catch (e: CancellationException) {
            throw e // Rethrow CancellationException so we properly receive cancel signals
        } catch (e: Throwable) {
            exceptionHandler.onException(this@Bot, e)
            fallback
        }
    }

    private var nextUpdateOffset: Long = -1

    private suspend fun pollUpdates(timeoutSeconds: Long): List<Update> {
        val updates = doGuarded(emptyList()) {
            call<List<Update>>("getUpdates") {
                put("timeout", timeoutSeconds)

                if (nextUpdateOffset >= 0) {
                    put("offset", nextUpdateOffset)
                }

                val updates = allowedUpdates
                if (updates != null) {
                    put("allowed_updates", updates.map { it.name })
                }
            }
        }

        if (updates.isNotEmpty()) {
            nextUpdateOffset = updates.last().id + 1
        }

        return updates
    }

    private fun tryCancel() {
        if (!scope.isActive) {
            throw CancellationException()
        }
    }

    private fun lifecycle(action: suspend LifecycleListener.(Bot) -> Unit) {
        scope.launch {
            try {
                lifecycleListener.action(this@Bot)
            } catch (e: Throwable) {
                exceptionHandler.onException(this@Bot, e)
            }
        }
    }

    private inline fun lifecycleImmediate(action: LifecycleListener.(Bot) -> Unit) {
        try {
            lifecycleListener.action(this)
        } catch (e: Throwable) {
            exceptionHandler.onException(this, e)
        }
    }

    private suspend fun doPollLoop() {
        while (!stop) {
            tryCancel()

            if (pauseUpdates) {
                pauseUpdatesCondition.wait()
            } else {
                val updates = pollUpdates(pollInterval)

                for (update in updates) {
                    tryCancel()

                    doGuarded {
                        updateListener.onUpdate(this, update)
                    }
                }
            }
        }
    }

    private suspend fun doOccasionLoop() {
        lastOccasion = Now.s()
        while (!stop) {
            tryCancel()

            lifecycle { onOccasion(it) }
            delay(occasionInterval * 1000)
        }
    }

    private suspend fun doInputLoop() {
        while (!stop) {
            tryCancel()

            val ln = input.getlnOrNull() ?: return

            lifecycle { onInput(it, ln) }
        }
    }

    /**
     * Starts the bot's poll loop in a suspending manner. The bot will run in its own coroutine scope.
     * This method will not return until another coroutine or thread stops the bot, until the bot stops itself,
     * or until external influence stops the calling coroutine.
     */
    suspend fun run() {
        // Initialise
        // --------------------

        // This happens in a Mutex so that upon a race condition of two run calls, only one ever gets to succeed.
        // Once out of the Mutex, running is true so the next run call to get the Mutex will throw.
        initMutex.withLock {
            if (running) {
                throw IllegalStateException("Already running")
            }

            scope = CoroutineScope(currentCoroutineContext())

            pollLoop = Future()
            occasionLoop = Future()
            inputLoop = Future()
            input = Input()

            running = true
        }

        try {
            // Launch
            // --------------------

            // Update loop runs the long polling to the Telegram API
            pollLoop.set(scope.launch {
                waitReady()
                doPollLoop()
            })

            // Occasion loop runs occasional lifecycle calls
            occasionLoop.set(scope.launch {
                waitReady()
                doOccasionLoop()
            })

            // Input loop reads console input
            inputLoop.set(scope.launch {
                waitReady()
                doInputLoop()
            })


            // Signal ready
            // --------------------
            ready.setDone()


            // Run lifecycle
            // --------------------
            try {
                lifecycle { onReady(it) }
                join()
                lifecycle { onStop(it) }
            } catch (e: CancellationException) {
                lifecycleImmediate { onHalt(it) }
                throw e
            }
        } finally {
            // Cleanup
            // --------------------

            // Reinitialise what we need in the second round
            ready = Future()

            stop = false
            running = false
        }
    }

    /**
     * Starts the bot's poll loop in a blocking manner, blocking the current thread from execution until the bot stops.
     * This method will not return until another thread stops the bot, until the bot stops itself, or until
     * external influence stops the calling thread.
     */
    fun runBlocking() {
        runBlocking {
            run()
        }
    }

    /**
     * Starts the bot's poll loop in a blocking manner, blocking the current thread from execution until the bot stops.
     * This method will not return until another thread stops the bot, until the bot stops itself, or until
     * external influence stops the calling thread.
     */
    fun runBlocking(context: CoroutineContext) {
        runBlocking(context) {
            run()
        }
    }

    /**
     * Starts the bot's poll loop on a new thread, as by calling [runBlocking] on a new thread.
     * This method will return the started thread.
     */
    fun runAsync(): Thread = thread {
        runBlocking()
    }

    /**
     * Starts the bot's poll loop on a new thread, as by calling [runBlocking] on a new thread.
     * This method will return the started thread.
     */
    fun runAsync(context: CoroutineContext): Thread = thread {
        runBlocking(context)
    }

    /**
     * Suspends until the bot is in a ready state. Calls cannot be made using the bot until a ready state is reached.
     *
     * When this method is called before the bot is started, the method will suspend indefinitely until the bot is started.
     */
    suspend fun waitReady() {
        ready.get()
    }

    /**
     * Blocks until the bot is in a ready state. Calls cannot be made using the bot until a ready state is reached.
     *
     * When this method is called before the bot is started, the method will block indefinitely until the bot is started.
     */
    fun waitReadyBlocking() {
        runBlocking {
            waitReady()
        }
    }

    /**
     * Blocks until the bot is in a ready state. Calls cannot be made using the bot until a ready state is reached.
     *
     * When this method is called before the bot is started, the method will block indefinitely until the bot is started.
     */
    fun waitReadyBlocking(context: CoroutineContext) {
        runBlocking(context) {
            waitReady()
        }
    }

    /**
     * Suspends until the bot finishes. If no [stop] has been called, this method will not return until some other
     * coroutine or thread stops the bot, until the bot stops itself, or until external influence stops the calling
     * coroutine.
     *
     * If the bot is not running, this method will immediately return, even if the bot was never started.
     */
    suspend fun join() {
        if (!running) {
            return
        }

        pollLoop.get().join()
        occasionLoop.get().join()
        inputLoop.get().join()
    }

    /**
     * Blocks the thread until the bot finishes. If no [stop] has been called, this method will not return until some
     * other thread stops the bot, or until the bot stops itself, or until external influence stops the calling thread.
     *
     * If the bot is not running, this method will immediately return, even if the bot was never started.
     */
    fun joinBlocking() {
        runBlocking {
            join()
        }
    }

    /**
     * Blocks the thread until the bot finishes. If no [stop] has been called, this method will not return until some
     * other thread stops the bot, or until the bot stops itself, or until external influence stops the calling thread.
     *
     * If the bot is not running, this method will immediately return, even if the bot was never started.
     */
    fun joinBlocking(context: CoroutineContext) {
        runBlocking(context) {
            join()
        }
    }

    /**
     * Signals the bot to stop. When `halt` is set to `true`, the bot's coroutine will be halted immediately.
     * When `halt` is omitted or set to `false`, the bot will process updates once more and gracefully shut down.
     *
     * This method suspends until the bot is ready, since the bot cannot be stopped before it is ready. If the bot is not running,
     * then this method will return immediately.
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    suspend fun signalStop(halt: Boolean = false) {
        if (!running) {
            return
        }

        waitReady()

        stop = true
        input.stop()

        if (halt) {
            pollLoop.get().cancel()
            occasionLoop.get().cancel()
            inputLoop.get().cancel()
        }
    }

    /**
     * Signals the bot to stop. When `halt` is set to `true`, the bot's coroutine will be halted immediately.
     * When `halt` is omitted or set to `false`, the bot will process updates once more and gracefully shut down.
     *
     * This method suspends until the bot is ready, since the bot cannot be stopped before it is ready. If the bot is not running,
     * then this method will return immediately.
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun signalStopBlocking(halt: Boolean = false) {
        if (!running) {
            return
        }

        runBlocking {
            signalStop(halt)
        }
    }

    /**
     * Signals the bot to stop. When `halt` is set to `true`, the bot's coroutine will be halted immediately.
     * When `halt` is omitted or set to `false`, the bot will process updates once more and gracefully shut down.
     *
     * This method suspends until the bot is ready, since the bot cannot be stopped before it is ready. If the bot is not running,
     * then this method will return immediately.
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun signalStopBlocking(context: CoroutineContext, halt: Boolean = false) {
        if (!running) {
            return
        }

        runBlocking(context) {
            signalStop(halt)
        }
    }

    /**
     * Signals the bot to stop, as per [signalStop], and then suspends until the bot has stopped, as per [join].
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    suspend fun stop(halt: Boolean = false) {
        signalStop(halt)
        join()
    }

    /**
     * Signals the bot to stop, as per [signalStop], and then blocks until the bot has stopped, as per [joinBlocking].
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun stopBlocking(halt: Boolean = false) {
        signalStopBlocking(halt)
        joinBlocking()
    }

    /**
     * Signals the bot to stop, as per [signalStop], and then blocks until the bot has stopped, as per [joinBlocking].
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun stopBlocking(context: CoroutineContext, halt: Boolean = false) {
        signalStopBlocking(context, halt)
        joinBlocking(context)
    }
}

