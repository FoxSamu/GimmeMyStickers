/*
 * Copyright (c) 2025 Olaf W. Nankman.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("NOTHING_TO_INLINE")

package net.foxboi.gms

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.reflect.KClass

class Log(val logger: Logger) {
    inline fun trace(msg: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(msg())
        }
    }

    inline fun debug(msg: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(msg())
        }
    }

    inline fun info(msg: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(msg())
        }
    }

    inline fun warn(msg: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(msg())
        }
    }

    inline fun error(msg: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(msg())
        }
    }

    inline fun fatal(msg: () -> String) {
        if (logger.isFatalEnabled) {
            logger.fatal(msg())
        }
    }

    inline fun trace(e: Throwable, msg: () -> String) {
        if (logger.isTraceEnabled) {
            logger.trace(msg(), e)
        }
    }

    inline fun debug(e: Throwable, msg: () -> String) {
        if (logger.isDebugEnabled) {
            logger.debug(msg(), e)
        }
    }

    inline fun info(e: Throwable, msg: () -> String) {
        if (logger.isInfoEnabled) {
            logger.info(msg(), e)
        }
    }

    inline fun warn(e: Throwable, msg: () -> String) {
        if (logger.isWarnEnabled) {
            logger.warn(msg(), e)
        }
    }

    inline fun error(e: Throwable, msg: () -> String) {
        if (logger.isErrorEnabled) {
            logger.error(msg(), e)
        }
    }

    inline fun fatal(e: Throwable, msg: () -> String) {
        if (logger.isFatalEnabled) {
            logger.fatal(msg(), e)
        }
    }

    inline fun trace(e: Throwable) {
        if (logger.isTraceEnabled) {
            logger.trace(e)
        }
    }

    inline fun debug(e: Throwable) {
        if (logger.isDebugEnabled) {
            logger.debug(e)
        }
    }

    inline fun info(e: Throwable) {
        if (logger.isInfoEnabled) {
            logger.info(e)
        }
    }

    inline fun warn(e: Throwable) {
        if (logger.isWarnEnabled) {
            logger.warn(e)
        }
    }

    inline fun error(e: Throwable) {
        if (logger.isErrorEnabled) {
            logger.error(e)
        }
    }

    inline fun fatal(e: Throwable) {
        if (logger.isFatalEnabled) {
            logger.fatal(e)
        }
    }
}

fun Log(cls: KClass<*>) = Log(LogManager.getLogger(cls.java))
fun Log(name: String) = Log(LogManager.getLogger(name))
inline fun <reified T> Log() = Log(T::class)

inline val <reified T> T.Log get() = Log<T>()