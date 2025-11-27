package net.foxboi.gms

object Now {
    fun ms(): Long {
        return System.currentTimeMillis()
    }

    fun s(): Long {
        return ms() / 1000
    }
}