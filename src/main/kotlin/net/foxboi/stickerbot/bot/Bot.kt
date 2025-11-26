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
import kotlinx.io.RawSource
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import net.foxboi.stickerbot.Log
import net.foxboi.stickerbot.util.Barrier
import net.foxboi.stickerbot.util.Gate
import net.foxboi.stickerbot.util.open
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

/**
 * A [Bot] class wraps the Telegram Bot API and encapsulates all the core logic required to run a Telegram bot. The [Bot] class...
 * - Manages a proper lifecycle, which can be hooked into using [LifecycleListener].
 * - Fetches updates using long polling and pass them to an [UpdateListener].
 * - Handles exceptions caused by any listener and pass them to an [ExceptionHandler].
 * - Provides an easy interface to format API calls.
 *
 * To create a bot, you need a bot token from [@BotFather](https://t.me/BotFather). Make sure to never leak this token. Once you have a token,
 * create a bot, configure it as necessary, and call [run].
 *
 * Here is a very simple bot that will print the username of the bot account its token is associated to, and then stops:
 * ```kotlin
 * suspend fun main() {
 *     // Create bot
 *     val bot = Bot("12345678:aBcdEFghijK1LmnOPQrstuv2wxyzAB3cd")
 *
 *     bot.runAsync()   // Run the bot on a new thread
 *     bot.waitReady()  // Suspend until the bot is ready
 *
 *     val me = bot.getMe() // Make bot call
 *     println("Connected to Bot API, my username is ${me.username}")
 *
 *     bot.stop() // Stop the bot and suspend until it stops
 * }
 * ```
 *
 * Here is another bot that prints every message it receives:
 * ```kotlin
 * // Create an update listener
 * object Listener : UpdateListener {
 *     override suspend fun onMessage(bot: Bot, update: MessageUpdate) {
 *         // Print every received message
 *         println("Received a message: ${update.message.text}")
 *     }
 * }
 *
 * suspend fun main() {
 *     // Create bot
 *     val bot = Bot("12345678:aBcdEFghijK1LmnOPQrstuv2wxyzAB3cd")
 *
 *     bot.updateListener = Listener  // Set the bot listener
 *     bot.run()  // Run the bot, and suspend
 * }
 * ```
 */
class Bot(
    private val token: String
) {
    private val log = Log

    /**
     * The [UpdateListener] that will receive [Update]s from this bot. Default is [UpdateIgnorer].
     */
    var updateListener: UpdateListener = UpdateIgnorer

    /**
     * The [LifecycleListener] that will receive lifecycle updates of this bot. Default is [LifecycleIgnorer].
     */
    var lifecycleListener: LifecycleListener = LifecycleIgnorer

    /**
     * The [ExceptionHandler] that will handle exceptions of this bot. Default is [ExceptionPrinter].
     */
    var exceptionHandler: ExceptionHandler = ExceptionPrinter

    /**
     * The maximum amount of seconds that the bot will wait for when polling updates. When the bot polls updates, it requests the Telegram API to wait
     * this amount of seconds before the API should return an empty list of updates. If any updates arrive within this time, they will be returned
     * immediately.
     *
     * Higher values will mean longer stop times and potentially can exhaust the HTTP request time, causing an HTTP timeout error. Lower values will
     * cause more requests to be made to the Telegram API, potentially making the bot reach rate limit.
     */
    var pollTimeout: Long = 10L

    /**
     * The amount of seconds between occasional lifecycle updates, which can be intercepted with [LifecycleListener.onOccasion].
     */
    var occasionInterval: Long = 3L

    /**
     * The set of allowed [UpdateType]s.
     */
    private var allowedUpdates: Set<UpdateType>? = setOf()

    /**
     * A barrier for updates. This is closed with [pauseUpdates] and reopened with [resumeUpdates].
     */
    private val updateBarrier = Barrier(open = true)


    /**
     * Mutex for initialization, to avoid race conditions when multiple threads try to start the bot, or threads trying to stop the bot during
     * initialization.
     */
    private val initMutex = Mutex()

    /** Marks that the bot is running. */
    private var running = false

    /** Marks that the bot is supposed to stop. */
    private var stop = false

    /** Marks that the bot is supposed to halt. */
    private var halt = false


    /**
     * Ready gate. Opened once the bot is ready to unsuspend [waitReady] calls.
     */
    private var ready = Gate<Unit>()

    /** [Gate] supplying the poll loop's [Job]. */
    private lateinit var pollLoop: Gate<Job>

    /** [Gate] supplying the occasion loop's [Job]. */
    private lateinit var occasionLoop: Gate<Job>

    /** [Gate] supplying the input loop's [Job]. */
    private lateinit var inputLoop: Gate<Job>

    /** [Input] to receive input from StdIn. */
    private lateinit var input: Input

    /** The ID offset to use in the next update call. */
    private var nextUpdateOffset: Long = -1


    /** The [CoroutineScope] in which the bot runs. */
    private lateinit var scope: CoroutineScope

    /**
     * A [Json] instance used by the bot to deserialize and serialize objects going through the Telegram API.
     */
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
    }

    /**
     * The [HttpClient] used to make calls to the Telegram API.
     */
    private lateinit var http: HttpClient

    /**
     * Handles a [call] or [push] response.
     * @param response The response to handle.
     * @param deserializer The deserializer for the body.
     */
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
    @Suppress("UNUSED")
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

    /**
     * Fetches a file from the Telegram API, returning a [RawSource] that can be used to read the file. To get the file path, call [Bot.getFile] to
     * obtain a [File] and pass [File.filePath] as argument.
     *
     * @param filePath The file path of the file to download.
     * @return The [RawSource] to read the file.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     */
    suspend fun pull(
        filePath: String
    ): RawSource {
        if (!ready.opened) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.get("https://api.telegram.org/file/bot$token/$filePath")

        if (!response.status.isSuccess()) {
            throw HttpException(response.status.value, response.status.description)
        }

        return response.bodyAsChannel().asSource()
    }

    /**
     * Invokes an endpoint on the Telegram API, like [call], but using a `multipart/form-data` body rather than a JSON body. If you aren't uploading
     * any files in your API call, consider using [call] to reduce the overhead of generating and sending form data.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param resultDeserializer A deserializer for the result.
     * @param push A request builder. Called in place to set up the request.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
    suspend fun <R> push(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        push: PushBuilder.() -> Unit
    ): R {
        if (!ready.opened) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.post("https://api.telegram.org/bot$token/$endpoint") {
            val data = withContext(Dispatchers.IO) {
                formData { PushBuilder(this, json).push() }
            }

            setBody(MultiPartFormDataContent(data))
        }
        return handleResponse(response, resultDeserializer)
    }

    /**
     * Invokes an endpoint on the Telegram API, like [call], but using a `multipart/form-data` body rather than a JSON body. If you aren't uploading
     * any files in your API call, consider using [call] to reduce the overhead of generating and sending form data.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param push A request builder. Called in place to set up the request.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
    suspend inline fun <reified R> push(
        endpoint: String,
        noinline push: PushBuilder.() -> Unit
    ): R {
        return push(endpoint, json.serializersModule.serializer(), push)
    }

    /**
     * Invokes an endpoint on the Telegram API, by sending a `POST` request with a JSON body. This does not support uploading files. If files need
     * to be uploaded, use [push] instead.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param resultDeserializer A deserializer for the result.
     * @param input The JSON body to send to the API.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
    suspend fun <R> call(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        input: JsonObject
    ): R {
        if (!ready.opened) {
            throw IllegalStateException("Not yet ready")
        }

        val response = http.post("https://api.telegram.org/bot$token/$endpoint") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(input))
        }
        return handleResponse(response, resultDeserializer)
    }

    /**
     * Invokes an endpoint on the Telegram API, by sending a `POST` request with a JSON body. This does not support uploading files. If files need
     * to be uploaded, use [push] instead.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param resultDeserializer A deserializer for the result.
     * @param input A request builder. Called in place to set up the request.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
    suspend inline fun <R> call(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        input: QueryBuilder.() -> Unit = {}
    ): R {
        return call(endpoint, resultDeserializer, buildJsonObject {
            input(QueryBuilder(this, json))
        })
    }

    /**
     * Invokes an endpoint on the Telegram API, by sending a `POST` request with a JSON body. This does not support uploading files. If files need
     * to be uploaded, use [push] instead.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param input The JSON body to send to the API.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
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

    /**
     * Invokes an endpoint on the Telegram API, by sending a `POST` request with a JSON body. This does not support uploading files. If files need
     * to be uploaded, use [push] instead.
     *
     * @param endpoint The endpoint name to call. The called endpoint will be `https://api.telegram.org/bot<token>/<endpoint>`.
     *     For example, to call `getMe`, pass `"getMe"`.
     * @param input A request builder. Called in place to set up the request.
     *
     * @throws IllegalStateException When the bot is not ready yet.
     * @throws HttpException When an erroneous HTTP status code was returned (4XX or 5XX).
     * @throws TelegramException When the Telegram API returned an error code.
     * @throws kotlinx.serialization.SerializationException When deserialization of the body fails.
     */
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

    /**
     * Pauses the bot from fetching any updates until it is resumed with [resumeUpdates].
     */
    fun pauseUpdates() {
        updateBarrier.tryClose()
    }

    /**
     * Resumes the bot from [pauseUpdates].
     */
    fun resumeUpdates() {
        updateBarrier.tryOpen()
    }


    /**
     * Catch any exceptions in the given action and pass them to [exceptionHandler].
     */
    private inline fun doGuarded(action: () -> Unit) {
        doGuarded(Unit, action)
    }

    /**
     * Catch any exceptions in the given action and pass them to [exceptionHandler]. Returns `fallback` when an exception is caught, otherwise it
     * returns the action's result.
     */
    private inline fun <T> doGuarded(fallback: T, action: () -> T): T {
        return try {
            action()
        } catch (e: CancellationException) {
            throw e // Rethrow CancellationException so we properly receive cancel signals
        } catch (e: Throwable) {
            log.trace(e) { "Handling exception" }

            exceptionHandler.onException(this@Bot, e)
            fallback
        }
    }


    /**
     * Performs one update poll.
     */
    private suspend fun pollUpdates(): List<Update> {
        // Get updates from Telegram API
        log.trace { "Fetching updates" }
        val updates = doGuarded(emptyList()) {
            call<List<Update>>("getUpdates") {
                put("timeout", pollTimeout)

                if (nextUpdateOffset >= 0) {
                    put("offset", nextUpdateOffset)
                }

                val updates = allowedUpdates
                if (updates != null) {
                    put("allowed_updates", updates.map { it.name })
                }
            }
        }

        // Process updates
        log.trace { "Processing ${updates.size} updates" }
        if (updates.isNotEmpty()) {
            nextUpdateOffset = updates.last().id + 1
        }

        return updates
    }


    /**
     * Launches a lifecycle event. To avoid lifecycle events from cancelling themselves when they call [signalStop],
     * lifecycle events are always launched as a new coroutine, which isn't cancelled when the bot is halted.
     */
    private fun lifecycle(logName: String, action: suspend LifecycleListener.(Bot) -> Unit) {
        scope.launch {
            lifecycleInPlace(logName, action)
        }
    }

    /**
     * Launches a lifecycle event in place, unlike [lifecycle], which launches the event on a new coroutine.
     */
    private suspend fun lifecycleInPlace(logName: String, action: suspend LifecycleListener.(Bot) -> Unit) {
        log.trace { "Lifecycle: $logName" }
        doGuarded {
            lifecycleListener.action(this@Bot)
        }
    }

    /**
     * Runs the poll loop.
     */
    private suspend fun doPollLoop() {
        log.trace { "Poll loop started" }

        while (!stop) {
            // If paused, wait until updates are resumed
            updateBarrier.wait()

            for (update in pollUpdates()) {
                doGuarded {
                    updateListener.onUpdate(this, update)
                }
            }
        }
    }

    /**
     * Runs the occasion loop.
     */
    private suspend fun doOccasionLoop() {
        log.trace { "Occasion loop started" }

        while (!stop) {
            lifecycle("OCCASION") { onOccasion(it) }
            delay(occasionInterval * 1000)
        }
    }

    /**
     * Runs the input loop.
     */
    private suspend fun doInputLoop() {
        log.trace { "Input loop started" }

        while (!stop) {
            val ln = input.getlnOrNull() ?: return

            lifecycle("INPUT") { onInput(it, ln) }
        }
    }

    /**
     * Runs the bot initialization phase. This is the first thing the bot does when it gets started.
     * In this phase necessary fields are initialized before setting [running] to true.
     *
     * This phase also deals with race conditions where the bot is attempted to be ran twice.
     */
    private suspend fun doInit() = initMutex.withLock {
        // This happens in a Mutex so that in a race condition of two doInit calls, only one ever gets to succeed.
        // Once out of the Mutex, running = true, so the next doInit call to get the Mutex lock will throw.

        if (running) {
            throw IllegalStateException("Already running")
        }

        log.trace { "Initializing bot" }

        scope = CoroutineScope(currentCoroutineContext())

        nextUpdateOffset = -1
        pollLoop = Gate()
        occasionLoop = Gate()
        inputLoop = Gate()
        input = Input()

        http = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        running = true
    }

    /**
     * Runs the bot launch phase. This happens after [doInit] to launch all the necessary coroutines that run the bot.
     * Launched coroutines will all wait for the bot to reach the ready state, so whatever they do happens during the next phase, [doLifecycle].
     */
    private fun doLaunch() {
        log.trace { "Bot is running, launching update loops" }

        // Update loop runs the long polling to the Telegram API
        pollLoop.open(scope.launch {
            log.trace { "Starting poll loop" }
            waitReady()
            doPollLoop()
        })

        // Occasion loop runs occasional lifecycle calls
        occasionLoop.open(scope.launch {
            log.trace { "Starting occasion loop" }
            waitReady()
            doOccasionLoop()
        })

        // Input loop reads console input
        inputLoop.open(scope.launch {
            log.trace { "Starting input loop" }
            waitReady()
            doInputLoop()
        })
    }

    /**
     * Runs the bot lifecycle phase. This happens after [doLaunch] and it will run until the bot is stopped.
     * Any event called on [LifecycleListener] is called during this phase.
     */
    private suspend fun doLifecycle() {
        ready.open()
        log.trace { "Bot is ready" }

        lifecycle("READY") { onReady(it) }

        pollLoop.wait().join()
        log.trace { "Poll loop stopped" }

        occasionLoop.wait().join()
        log.trace { "Occasion loop stopped" }

        inputLoop.wait().join()
        log.trace { "Input loop stopped" }

        // Stop must happen in place otherwise the goes into doFinalize and close the HTTP client before the stop handler is done
        lifecycleInPlace("STOP") { onStop(it) }
    }

    /**
     * Runs the bot finalize phase. This happens after [doLifecycle], it cleans up any used resources and prepares the bot for another run, if
     * desired.
     */
    private fun doFinalize() {
        log.trace { "Finalizing" }

        http.close()

        // Reinitialise what we need in the second round
        ready = Gate()
        stop = false
        halt = false
        running = false
    }

    /**
     * Starts the bot's poll loop in a suspending manner. The bot will run in its own coroutine scope.
     * This method will not return until another coroutine or thread stops the bot, until the bot stops itself,
     * or until external influence stops the calling coroutine.
     */
    suspend fun run() {
        doInit()

        try {
            doLaunch()
            doLifecycle()
        } finally {
            doFinalize()
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
        ready.wait()
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

        pollLoop.wait().join()
        occasionLoop.wait().join()
        inputLoop.wait().join()
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
    suspend fun signalStop(halt: Boolean = false) = initMutex.withLock {
        // Run in `initMutex` to avoid stops during initialization phase.
        if (!running || stop) {
            return
        }

        waitReady()

        this.stop = true
        this.halt = halt

        input.stop()

        if (halt) {
            pollLoop.wait().cancel()
            occasionLoop.wait().cancel()
            inputLoop.wait().cancel()
        }
    }

    /**
     * Signals the bot to stop. When `halt` is set to `true`, the bot's coroutine will be halted immediately.
     * When `halt` is omitted or set to `false`, the bot will process updates once more and gracefully shut down.
     *
     * This method blocks until the bot is ready, since the bot cannot be stopped before it is ready. If the bot is not running,
     * then this method will return immediately.
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun signalStopBlocking(halt: Boolean = false) {
        // It may be the case that `stop` is false here, but true once it reaches the `signalStop` call.
        // This is a race condition, but it's harmless because this same condition check is performed again within
        // `signalStop` but in a proper mutex lock. This check is merely here to avoid unnecessarily starting a coroutine.
        if (!running || stop) {
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
        // Harmless race condition here. See above.
        if (!running || stop) {
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

