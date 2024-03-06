import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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

	runtimeOnly("io.grpc:grpc-netty:1.38.1")
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	relocate("android", "fi.joniaromaa.bouncer.libs.android")

	relocate("com.google.api", "fi.joniaromaa.bouncer.libs.com.google.api")
	relocate("com.google.cloud", "fi.joniaromaa.bouncer.libs.com.google.cloud")
	relocate("com.google.common", "fi.joniaromaa.bouncer.libs.com.google.common")
	relocate("com.google.errorprone", "fi.joniaromaa.bouncer.libs.com.google.errorprone")
	relocate("com.google.geo", "fi.joniaromaa.bouncer.libs.com.google.geo")
	relocate("com.google.gson", "fi.joniaromaa.bouncer.libs.com.google.gson")
	relocate("com.google.j2objc", "fi.joniaromaa.bouncer.libs.com.google.j2objc")
	relocate("com.google.logging", "fi.joniaromaa.bouncer.libs.com.google.logging")
	relocate("com.google.longrunning", "fi.joniaromaa.bouncer.libs.com.google.longrunning")
	relocate("com.google.protobuf", "fi.joniaromaa.bouncer.libs.com.google.protobuf")
	relocate("com.google.rpc", "fi.joniaromaa.bouncer.libs.com.google.rpc")
	relocate("com.google.thirdparty", "fi.joniaromaa.bouncer.libs.com.google.thirdparty")
	relocate("com.google.type", "fi.joniaromaa.bouncer.libs.com.google.type")

	relocate("_COUROUTINE", "fi.joniaromaa.bouncer.libs._COUROUTINE")
	relocate("io", "fi.joniaromaa.bouncer.libs.io")
	relocate("javax.annotation", "fi.joniaromaa.bouncer.libs.javax.annotation")
	relocate("kotlin", "fi.joniaromaa.bouncer.libs.kotlin")
	relocate("kotlinx", "fi.joniaromaa.bouncer.libs.kotlinx")
	relocate("org", "fi.joniaromaa.bouncer.libs.org")
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
