plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Для HTTP-запросов
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // implementation("com.google.android.gms:play-services-fido:21.2.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.23")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}