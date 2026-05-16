plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
}

group = "com.bowerzlabs"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.9")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}