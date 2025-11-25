import java.io.FileReader
import java.util.*

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"

    application
}

group = "net.foxboi"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
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