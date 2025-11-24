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
     * Called on occasion by the [Bot]. This can be used to occasionally save things.
     */
    suspend fun onOccasion(bot: Bot) {

    }

    /**
     * Called by the [Bot] to indicate that it was requested to stop gracefully, with no immediate cancellation.
     * Bot calls can still be made from this method.
     */
    suspend fun onStop(bot: Bot) {

    }

    /**
     * Called by the [Bot] to indicate that it was requested to stop immediately via cancellation.
     * Bot calls cannot be made from this method.
     */
    fun onHalt(bot: Bot) {

    }
}

object LifecycleIgnorer : LifecycleListener