import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
}

group = "fi.joniaromaa"
version = "1.0-SNAPSHOT"

repositories {
	maven {
		name = "papermc"
		url = uri("https://papermc.io/repo/repository/maven-public/")
	}
}

dependencies {
	implementation(project(":bouncer-common"))

	compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")

	implementation("org.spongepowered:configurate-yaml:3.7.1")
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	relocate("com.google", "fi.joniaromaa.bouncer.libs.com.google")
	relocate("io", "fi.joniaromaa.bouncer.libs.io")
	relocate("javax.annotation", "fi.joniaromaa.bouncer.libs.javax.annotation")
	relocate("kotlin", "fi.joniaromaa.bouncer.libs.kotlin")
	relocate("kotlinx", "fi.joniaromaa.bouncer.libs.kotlinx")
	relocate("ninja", "fi.joniaromaa.bouncer.libs.ninja")
	relocate("org", "fi.joniaromaa.bouncer.libs.org") {
		exclude("org.bukkit.**")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
