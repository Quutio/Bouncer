import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.PluginDependency

plugins {
    `java-library`
    kotlin("jvm")
    id("org.spongepowered.gradle.plugin") version "1.1.1"
    id("com.github.johnrengelman.shadow")
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
    apiVersion("8.0.0")
    plugin("bouncer") {
        loader(PluginLoaders.JAVA_PLAIN)
        displayName("Bouncer")
        mainClass("fi.joniaromaa.bouncer.sponge.SpongeBouncerPlugin")
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

tasks.create<com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks["shadowJar"] as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
    prefix = "fi.joniaromaa.bouncer.libs"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    dependsOn(tasks["relocateShadowJar"])

    mergeServiceFiles()
}

tasks.named("assemble").configure {
    dependsOn("shadowJar")
}