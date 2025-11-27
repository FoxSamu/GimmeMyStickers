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
    mainClass = "net.foxboi.stickerbot.MainKt"
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
            package net.foxboi.stickerbot
            
            object BuildInfo {
                const val GROUP = "$group"
                const val NAME = "$name"
                const val VERSION = "$version"
            }
        """.trimIndent()
        )
    }
}
