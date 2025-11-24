package net.foxboi.stickerbot

object Env {
    private const val PREFIX = "STICKERBOT_"

    val token = this["TOKEN"]
    val storageDirectory = this["STORAGE_DIRECTORY", "."]
    val maxCachedSessions = this["MAX_CACHED_SESSIONS", "65535"].toIntOrNull() ?: 65535

    operator fun get(key: String, default: String? = null): String {
        return System.getenv(PREFIX + key)
            ?: default
            ?: throw Exception("Environment variable $key must be set in order to run")
    }
}