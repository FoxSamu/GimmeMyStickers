package net.foxboi.gms.storage

import kotlinx.io.Sink
import kotlinx.io.Source

interface StorageLocation {
    fun name(): String
    fun exists(): Boolean
    fun source(): Source
    fun sink(): Sink
    fun delete()
}