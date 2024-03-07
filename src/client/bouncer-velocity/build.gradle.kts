import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	alias(libs.plugins.kotlin.jvm)
	id(libs.plugins.kotlin.kapt.get().pluginId)
	alias(libs.plugins.shadow)
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

	compileOnly(libs.velocity)
	kapt(libs.velocity)
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

tasks {
	withType<ShadowJar> {
		mergeServiceFiles()

		relocate("com.google", "fi.joniaromaa.bouncer.libs.com.google") {
			exclude("com.google.inject.**")
		}
		relocate("io", "fi.joniaromaa.bouncer.libs.io")
		relocate("javax.annotation", "fi.joniaromaa.bouncer.libs.javax.annotation")
		relocate("kotlin", "fi.joniaromaa.bouncer.libs.kotlin")
		relocate("kotlinx", "fi.joniaromaa.bouncer.libs.kotlinx")
		relocate("org", "fi.joniaromaa.bouncer.libs.org")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
