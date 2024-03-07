import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
}

group = "io.quut"
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

	relocate("com.google", "io.quut.bouncer.libs.com.google")
	relocate("io", "io.quut.bouncer.libs.io") {
		exclude("io.quut.**")
	}
	relocate("javax.annotation", "io.quut.bouncer.libs.javax.annotation")
	relocate("kotlin", "io.quut.bouncer.libs.kotlin")
	relocate("kotlinx", "io.quut.bouncer.libs.kotlinx")
	relocate("ninja", "io.quut.bouncer.libs.ninja")
	relocate("org", "io.quut.bouncer.libs.org") {
		exclude("org.bukkit.**")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
