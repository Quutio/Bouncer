import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    kotlin("kapt")

    id("com.github.johnrengelman.shadow")
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        name = "velocity"
        url = uri("https://repo.velocitypowered.com/snapshots/")
    }
}

dependencies {
    implementation(project(":bouncer-common"))

    compileOnly("com.velocitypowered:velocity-api:3.1.0-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.1.0-SNAPSHOT")

    runtimeOnly("io.grpc:grpc-netty:1.38.1")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()

    isEnableRelocation = true
    relocationPrefix = "fi.joniaromaa.bouncer.libs"
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}