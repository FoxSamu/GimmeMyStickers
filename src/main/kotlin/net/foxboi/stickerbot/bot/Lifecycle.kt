package net.foxboi.stickerbot.bot

/**
 * Listener interface for lifecycle events of a [Bot].
 */
interface LifecycleListener {
    /**
     * Called by the [Bot] to indicate that it has reached its ready state. Bot calls can be made from this method.
     */
    suspend fun onReady(bot: Bot) {

    }

    /**
     * Called on occasion by the [Bot]. This can be used to occasionally save things. Bot calls can be made from this method.
     */
    suspend fun onOccasion(bot: Bot) {

    }

    /**
     * Called by the [Bot] when a line of input was received from the standard input. Bot calls can be made from this method.
     */
    suspend fun onInput(bot: Bot, input: String) {

    }

    /**
     * Called by the [Bot] to indicate that it was requested to stop gracefully, with no immediate cancellation.
     * Bot calls can still be made from this method.
     */
    suspend fun onStop(bot: Bot) {

    }

    /**
     * Called by the [Bot] to indicate that it was requested to stop immediately via cancellation.
     * Unlike [onStop], this method cannot run any coroutines and must only do the bare minimum that is necessary to halt safely.
     */
    fun onHalt(bot: Bot) {

    }
}

object LifecycleIgnorer : LifecycleListener