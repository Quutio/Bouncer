import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("org.spongepowered.gradle.plugin") version "2.0.0"
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":bouncer-common"))

    runtimeOnly("io.grpc:grpc-netty:1.38.1")
}

sponge {
    apiVersion("8.0.0-SNAPSHOT")
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("bouncer") {
        displayName("Bouncer")
        entrypoint("fi.joniaromaa.bouncer.sponge.SpongeBouncerPlugin")
        description("A load balancer for Minecraft servers")
        links {
            homepage("https://github.com/Quutio/Bouncer")
            source("https://github.com/Quutio/Bouncer")
            issues("https://github.com/Quutio/Bouncer/issues")
        }
        contributor("Joni Aromaa (isokissa3)") {
            description("Lead Developer")
        }
        contributor("Matias Paavilainen (Masa)") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

val javaTarget = 8 // Sponge targets a minimum of Java 8
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8" // Consistent source file encoding
        if (JavaVersion.current().isJava10Compatible) {
            release.set(javaTarget)
        }
    }
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()

    isEnableRelocation = true
    relocationPrefix = "fi.joniaromaa.bouncer.libs"
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}