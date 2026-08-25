import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.shadow)
}

repositories {
	maven {
		name = "papermc"
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencies {
	implementation(project(":bouncer-common"))

	compileOnly(libs.paper.api)

	implementation(libs.configurate.yaml)
}

tasks.withType<ShadowJar> {
	mergeServiceFiles()

	relocate("com.google", "io.quut.bouncer.libs.com.google")
	relocate("com.github.benmanes", "io.quut.bouncer.libs.com.github.benmanes")
	relocate("io", "io.quut.bouncer.libs.io") {
		exclude("io.quut.**")
	}
	relocate("javax.annotation", "io.quut.bouncer.libs.javax.annotation")
	relocate("kotlin", "io.quut.bouncer.libs.kotlin")
	relocate("kotlinx", "io.quut.bouncer.libs.kotlinx")
	relocate("org", "io.quut.bouncer.libs.org") {
		exclude("org.bukkit.**")
	}
}

tasks.named("assemble").configure {
	dependsOn("shadowJar")
}
