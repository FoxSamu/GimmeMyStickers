package net.foxboi.stickerbot.bot

/**
 * Listener interface for lifecycle events of a [Bot]. Set [Bot.lifecycleListener] to a [LifecycleListener] to receive lifecycle events.
 */
interface LifecycleListener {
    /**
     * Called by the [Bot] to indicate that it has reached its ready state. Bot calls can be made from this method.
     */
    suspend fun onReady(bot: Bot) {

    }

    /**
     * Called on occasion by the [Bot]. This can be used to occasionally save things. Bot calls can be made from this method.
     * The interval between calls to this method can be set with [Bot.occasionInterval].
     */
    suspend fun onOccasion(bot: Bot) {

    }

    /**
     * Called by the [Bot] when a line of input was received from the standard input. Bot calls can be made from this method.
     */
    suspend fun onInput(bot: Bot, input: String) {

    }

    /**
     * Called by the [Bot] to indicate that it has stopped.
     * Bot calls can still be made from this method, but the bot will not poll any new updates and will terminate once this method returns.
     */
    suspend fun onStop(bot: Bot) {

    }
}

object LifecycleIgnorer : LifecycleListener