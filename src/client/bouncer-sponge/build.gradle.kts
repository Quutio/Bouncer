import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
	alias(libs.plugins.sponge)
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

dependencies {
	implementation(project(":bouncer-common"))
}

sponge {
	apiVersion("8.3.0-SNAPSHOT")
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

val targetJava = 17
val targetJavaVersion = JavaVersion.toVersion(targetJava)
java {
	sourceCompatibility = targetJavaVersion
	targetCompatibility = targetJavaVersion
	if (JavaVersion.current() < targetJavaVersion) {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(targetJava))
		}
	}
}

tasks {
	withType<JavaCompile> {
		options.encoding = "UTF-8"
		options.release.set(targetJava)
	}

	withType<KotlinCompile> {
		kotlinOptions {
			jvmTarget = targetJava.toString()
		}
	}
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	relocate("com.google", "fi.joniaromaa.bouncer.libs.com.google") {
		exclude("com.google.inject.**")
	}
	relocate("io", "fi.joniaromaa.bouncer.libs.io")
	relocate("javax.annotation", "fi.joniaromaa.bouncer.libs.javax.annotation")
	relocate("kotlin", "fi.joniaromaa.bouncer.libs.kotlin")
	relocate("kotlinx", "fi.joniaromaa.bouncer.libs.kotlinx")
	relocate("org", "fi.joniaromaa.bouncer.libs.org") {
		exclude("org.spongepowered.**")
		exclude("org.apache.logging.**")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
