import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
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

    compileOnly("com.velocitypowered:velocity-api:3.0.1-SNAPSHOT")
    kapt("com.velocitypowered:velocity-api:3.0.1-SNAPSHOT")

    runtimeOnly("io.grpc:grpc-netty:1.38.1")
}

tasks.create<ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks["shadowJar"] as ShadowJar
    prefix = "fi.joniaromaa.bouncer.libs"
}

tasks.withType<ShadowJar> {
    dependsOn(tasks["relocateShadowJar"])

    mergeServiceFiles()
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}