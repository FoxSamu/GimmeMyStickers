package net.foxboi.gms.bot

/**
 * Thrown by [Bot.call] to indicate a Telegram API error, that is, a 2XX status code with an error result.
 */
class TelegramException(
    val errorCode: Int,
    val description: String? = null
) : Exception("Telegram error code #$errorCode" + if (description != null) ": $description" else "")

/**
 * Thrown by [Bot.call] to indicate an HTTP error, that is, a 4XX or 5XX status code.
 */
class HttpException(
    val statusCode: Int,
    val description: String? = null
) : Exception("HTTP status code #$statusCode" + if (description != null) ": $description" else "")

/**
 * Thrown by [Bot.waitReady] and [Bot.waitReadyBlocking] when the bot is halted before it could reach its ready state.
 */
class HaltedException() : Exception()

/**
 * A handler for exceptions that happen during the runtime of a [Bot]. These exceptions are normally not thrown, but
 * only printed (see [ExceptionPrinter]). To alter this behavior, set [Bot.exceptionHandler] to a custom
 * [ExceptionHandler].
 */
interface ExceptionHandler {
    fun onException(bot: Bot, e: Throwable)
}

/**
 * An [ExceptionHandler] that simply ignores all exceptions. Usage not recommended.
 */
object ExceptionIgnorer : ExceptionHandler {
    override fun onException(bot: Bot, e: Throwable) = Unit
}

/**
 * An [ExceptionHandler] that prints all exceptions as per [Throwable.printStackTrace].
 */
object ExceptionPrinter : ExceptionHandler {
    override fun onException(bot: Bot, e: Throwable) {
        e.printStackTrace()
    }
}