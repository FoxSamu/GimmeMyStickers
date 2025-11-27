package net.foxboi.gms.storage

import kotlinx.io.Sink
import kotlinx.io.Source
import net.foxboi.gms.Log

abstract class Storable(
    private val location: StorageLocation
) {
    private val log = Log

    private var changed = false

    val dirty get() = changed

    @Synchronized
    protected fun markDirty() {
        changed = true
    }

    @Synchronized
    protected fun markClean() {
        changed = false
    }

    @Synchronized
    fun save(force: Boolean = false) {
        if (force || changed) {
            location.sink().use {
                onSave(it)
            }
        }

        changed = false
    }

    @Synchronized
    fun load() {
        if (location.exists()) {
            try {
                location.source().use {
                    onLoad(it)
                }
                return
            } catch (e: Throwable) {
                log.error(e) { "Failed to load from store: ${location.name()}" }
            }
        }

        onInit()
        save(true) // Save immediately after init
    }

    @Synchronized
    fun delete() {
        location.delete()
    }

    protected abstract fun onInit()
    protected abstract fun onSave(output: Sink)
    protected abstract fun onLoad(input: Source)
}