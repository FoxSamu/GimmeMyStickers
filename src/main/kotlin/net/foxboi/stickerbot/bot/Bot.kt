package net.foxboi.stickerbot.bot

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import net.foxboi.stickerbot.Now
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Bot(
    private val token: String
) {
    var updateListener: UpdateListener = UpdateIgnorer
    var lifecycleListener: LifecycleListener = LifecycleIgnorer
    var exceptionHandler: ExceptionHandler = ExceptionPrinter

    var pollInterval: Long = 10L
    var minOccasionInterval: Long = 3L

    var allowedUpdates = setOf<UpdateType>()

    private var running = false
    private var stop = false

    private var ready = false
    private val readyLock = Any()
    private val readyContinuations = mutableListOf<Continuation<Unit>>()

    private var lastOccasion = -1L

    private lateinit var job: Job
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

    suspend fun <R> call(
        endpoint: String,
        resultDeserializer: DeserializationStrategy<R>,
        input: JsonObject
    ): R {
        if (!ready) {
            throw IllegalStateException("Bot is not yet ready")
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



    fun allowUpdates(first: UpdateType, vararg rest: UpdateType) {
        allowedUpdates = setOf(first, *rest)
    }

    fun allowUpdates(types: Iterable<UpdateType>) {
        allowedUpdates = types.toSet()
    }

    fun allowAllUpdates() {
        allowUpdates(UpdateType.all)
    }

    fun allowDefaultUpdates() {
        allowUpdates(emptyList())
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

                if (allowedUpdates.isNotEmpty()) {
                    put("allowed_updates", allowedUpdates.map { it.name })
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

    private suspend fun doReady() {
        synchronized(readyLock) {
            ready = true

            for (cont in readyContinuations) {
                cont.resume(Unit)
            }

            readyContinuations.clear()
        }
        lifecycleListener.onReady(this@Bot)
    }

    private suspend fun doStop() {
        if (scope.isActive) {
            lifecycleListener.onStop(this@Bot)
        } else {
            lifecycleListener.onHalt(this@Bot)
        }

        synchronized(readyLock) {
            ready = false

            val exc = HaltedException()
            for (cont in readyContinuations) {
                cont.resumeWithException(exc)
            }

            readyContinuations.clear()
        }
    }

    private suspend fun doPollLoop() {
        lastOccasion = Now.s()
        while (!stop) {
            tryCancel()

            if (Now.s() > lastOccasion + minOccasionInterval) {
                lastOccasion = Now.s()

                lifecycleListener.onOccasion(this)
            }

            val updates = pollUpdates(pollInterval)

            for (update in updates) {
                tryCancel()

                doGuarded {
                    updateListener.onUpdate(this, update)
                }
            }
        }
    }

    private fun ensureRunning() {
        if (!running) {
            throw IllegalStateException("Bot not running")
        }
    }

    /**
     * Starts the bot's poll loop in a suspending manner. The bot will run in its own coroutine scope.
     * This method will not return until another coroutine or thread stops the bot, until the bot stops itself,
     * or until external influence stops the calling coroutine.
     */
    suspend fun run() {
        if (running) {
            throw IllegalStateException("Already running")
        }

        running = true

        coroutineScope {
            job = launch {
                scope = this

                try {
                    doReady()
                    doPollLoop()
                } finally {
                    // Do this in a finally block to ensure that they happen even when cancelling
                    doStop()
                    stop = false
                    running = false
                }
            }
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
     * Starts the bot's poll loop on a new thread, as by calling [runBlocking] on a new thread.
     * This method will return the started thread.
     */
    fun runAsync(): Thread = thread {
        runBlocking()
    }

    /**
     * Suspends until the bot is in a ready state. Calls cannot be made using the bot until a ready state is reached.
     * When the bot is halted before it is ready, this method may throw a [HaltedException].
     */
    suspend fun waitReady() {
        ensureRunning()

        return suspendCoroutine { cont ->
            synchronized(readyLock) {
                if (ready) {
                    cont.resume(Unit)
                }

                readyContinuations += cont
            }
        }

    }

    /**
     * Blocks until the bot is in a ready state. Calls cannot be made using the bot until a ready state is reached.
     * When the bot is halted before it is ready, this method may throw a [HaltedException].
     */
    fun waitReadyBlocking() {
        ensureRunning()

        runBlocking {
            waitReady()
        }
    }

    /**
     * Suspends until the bot finishes. If no [stop] has been called, this method will not return until some other
     * coroutine or thread stops the bot, until the bot stops itself, or until external influence stops the calling
     * coroutine.
     */
    suspend fun join() {
        job.join()
    }

    /**
     * Blocks the thread until the bot finishes. If no [stop] has been called, this method will not return until some
     * other thread stops the bot, or until the bot stops itself, or until external influence stops the calling thread.
     */
    fun joinBlocking() {
        runBlocking {
            join()
        }
    }

    /**
     * Signals the bot to stop. When `halt` is set to `true`, the bot's coroutine will be halted immediately.
     * When `halt` is omitted or set to `false`, the bot will process updates once more and gracefully shut down.
     *
     * @param halt Whether to cancel the bot's coroutine immediately.
     */
    fun signalStop(halt: Boolean = false) {
        stop = true

        if (halt) {
            job.cancel()
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
        signalStop(halt)
        joinBlocking()
    }


}

