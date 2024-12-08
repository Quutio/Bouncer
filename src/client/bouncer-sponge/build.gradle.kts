import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
	alias(libs.plugins.sponge)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	implementation(project(":bouncer-common"))

	implementation(libs.harmony.sponge)
}

sponge {
	apiVersion("12.0.0")
	license("MIT")
	loader {
		name(PluginLoaders.JAVA_PLAIN)
		version("1.0")
	}
	plugin("bouncer") {
		entrypoint("io.quut.bouncer.sponge.SpongeBouncerPluginLoader")
		guiceModule("io.quut.bouncer.sponge.SpongeBouncerPluginLoader\$Module")
		displayName("Bouncer")
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

kotlin {
	jvmToolchain(21)
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	relocate("com.google", "io.quut.bouncer.libs.com.google") {
		exclude("com.google.inject.**")
	}
	relocate("com.github", "io.quut.bouncer.libs.com.github")
	relocate("io", "io.quut.bouncer.libs.io") {
		exclude("io.quut.bouncer.**")
	}
	relocate("javax.annotation", "io.quut.bouncer.libs.javax.annotation")
	relocate("kotlin", "io.quut.bouncer.libs.kotlin")
	relocate("kotlinx", "io.quut.bouncer.libs.kotlinx")
	relocate("org", "io.quut.bouncer.libs.org") {
		exclude("org.spongepowered.**")
		exclude("org.slf4j.**")
	}
	relocate("_COROUTINE", "io.quut.bouncer.libs._COROUTINE")
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
