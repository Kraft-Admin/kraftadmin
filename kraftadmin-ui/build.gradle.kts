plugins {
    base
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

val uiDir = projectDir.resolve("kraftadmin-ui")
val uiDistDir = uiDir.resolve("dist")
// Standard location for auto-serving static content in JARs
val resourcesDir = projectDir.resolve("src/main/resources/META-INF/resources/admin")

tasks.register<Exec>("npmInstall") {
    group = "build"
    workingDir = uiDir
    // Use ci for faster, more reliable builds in automated environments
    commandLine(if (org.apache.tools.ant.taskdefs.condition.Os.isFamily("windows")) listOf("npm.cmd", "install") else listOf("npm", "install"))

    inputs.file(uiDir.resolve("package.json"))
    outputs.dir(uiDir.resolve("node_modules"))
}

tasks.register<Exec>("buildUi") {
    group = "build"
    dependsOn("npmInstall")
    workingDir = uiDir
    commandLine(if (org.apache.tools.ant.taskdefs.condition.Os.isFamily("windows")) listOf("npm.cmd", "run", "build") else listOf("npm", "run", "build"))

    inputs.dir(uiDir.resolve("src"))
    outputs.dir(uiDistDir)
}

tasks.register<Sync>("embedUi") {
    group = "build"
    dependsOn("buildUi")

    // Use Sync instead of Copy to automatically remove old files in the destination
    from(uiDistDir)
    into(resourcesDir)
}

tasks.register<Exec>("generateIcons") {
    group = "build"
    dependsOn("npmInstall")
    workingDir = uiDir

    commandLine(if (org.apache.tools.ant.taskdefs.condition.Os.isFamily("windows"))
        listOf("node", "scripts/generate-icons.js")
    else listOf("node", "scripts/generate-icons.js"))

    // Force the task to always run by disabling the up-to-date check
    outputs.upToDateWhen { false }

    // Keep inputs/outputs defined for dependency tracking,
    // but the flag above ensures execution anyway.
    inputs.file(uiDir.resolve("package.json"))
    outputs.file(rootProject.projectDir.resolve("./kraftadmin-core/src/main/kotlin/com/kraftadmin/enums/KraftIcon.kt"))
}

// Ensure the UI is built before the resources are processed into the JAR
tasks.named("processResources") {
    dependsOn("embedUi")
}

kotlin {
    jvmToolchain(17)
}
