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

	runtimeOnly("io.grpc:grpc-netty:1.38.1")

	implementation("org.spongepowered:configurate-yaml:3.7.1")
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	isEnableRelocation = true
	relocationPrefix = "fi.joniaromaa.bouncer.libs"
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
