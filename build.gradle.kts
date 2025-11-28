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

import java.io.FileReader
import java.util.*

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"

    application
}

group = "net.foxboi"
version = "0.1"

val buildInfoDir = layout.buildDirectory.dir("buildInfo")

sourceSets {
    main {
        kotlin.srcDir(buildInfoDir)
    }
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

fun DependencyHandlerScope.skiko(version: String) {
    val osName: String = System.getProperty("os.name")

    val targetOs = when {
        osName == "Mac OS X" -> "macos"
        osName.startsWith("Win") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> error("Unsupported OS: $osName")
    }

    val targetArch = when (val osArch = System.getProperty("os.arch")) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported arch: $osArch")
    }

    val target = "${targetOs}-${targetArch}"
    implementation("org.jetbrains.skiko:skiko-awt-runtime-$target:$version")
}

dependencies {
    skiko("0.9.38")

    implementation("io.ktor:ktor-client-core:3.3.2")
    implementation("io.ktor:ktor-client-cio:3.3.2")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    runtimeOnly("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.2")
    implementation("org.apache.logging.log4j:log4j-api:2.25.2")
    implementation("org.apache.logging.log4j:log4j-core:2.25.2")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass = "net.foxboi.gms.MainKt"
}

tasks.run.configure {
    // Load .env
    val env = Properties()
    val envFile = file("$projectDir/.env")
    if (envFile.exists()) {
        FileReader(envFile, Charsets.UTF_8).use {
            env.load(it)
        }
    } else {
        logger.warn(".env not found, no environment variables were loaded")
    }

    environment("GMS_STORAGE_DIRECTORY", "$projectDir/run")
    environment(env.mapKeys { "${it.key}" })

    standardInput = System.`in`
}

tasks.test.configure {
    useJUnitPlatform()
}

tasks.compileKotlin.configure {
    dependsOn("buildInfo")
}

tasks.register("buildInfo") {
    inputs.property("group", project.group)
    inputs.property("name", project.name)
    inputs.property("version", project.version)

    outputs.dir(buildInfoDir)

    doLast {
        val file = file(buildInfoDir.get().file("BuildInfo.kt"))
        val group = inputs.properties["group"] as String
        val name = inputs.properties["name"] as String
        val version = inputs.properties["version"] as String

        file.parentFile?.mkdirs()

        file.writeText(
            """
            package net.foxboi.gms
            
            object BuildInfo {
                const val GROUP = "$group"
                const val NAME = "$name"
                const val VERSION = "$version"
            }
        """.trimIndent()
        )
    }
}
