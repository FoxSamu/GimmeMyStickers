plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.21"

    `java-library`
}

group = "net.foxboi"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}