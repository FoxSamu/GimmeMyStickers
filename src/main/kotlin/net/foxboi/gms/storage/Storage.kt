package net.foxboi.gms.storage

interface Storage<K, V : Storable> : AutoCloseable {
    fun get(key: K): V

    fun flush()
}