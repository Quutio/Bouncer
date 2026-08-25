plugins {
	alias(libs.plugins.kotlin.jvm)
	id(libs.plugins.kotlin.kapt.get().pluginId)
}

repositories {
	maven {
		name = "papermc"
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencies {
	compileOnly(project(":bouncer-api"))

	compileOnly(libs.velocity)
	kapt(libs.velocity)

	compileOnly(libs.omnivisor.api)
}

kotlin {
	jvmToolchain(25)
}
