plugins {
//    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
}

repositories {
    mavenCentral()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}


dependencies {
    api(project(":kraft-common"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    testImplementation(kotlin("test"))
}

evaluationDependsOn(":kraftadmin-ui")

val generateIconsTask = project(":kraftadmin-ui").tasks.named("generateIcons")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(generateIconsTask)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}