import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	alias(libs.plugins.kotlin.jvm)
	id(libs.plugins.kotlin.kapt.get().pluginId)
	alias(libs.plugins.shadow)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

repositories {
	maven {
		name = "velocity"
		url = uri("https://repo.velocitypowered.com/snapshots/")
	}
}

dependencies {
	implementation(project(":bouncer-common"))

	compileOnly(libs.velocity)
	kapt(libs.velocity)
}

kotlin {
	jvmToolchain(21)
}

tasks {
	withType<ShadowJar> {
		mergeServiceFiles()

		relocate("com.google", "io.quut.bouncer.libs.com.google") {
			exclude("com.google.inject.**")
		}
		relocate("io", "io.quut.bouncer.libs.io") {
			exclude("io.quut.**")
		}
		relocate("javax.annotation", "io.quut.bouncer.libs.javax.annotation")
		relocate("kotlin", "io.quut.bouncer.libs.kotlin")
		relocate("kotlinx", "io.quut.bouncer.libs.kotlinx")
		relocate("org", "io.quut.bouncer.libs.org")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
