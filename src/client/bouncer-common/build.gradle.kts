plugins {
	alias(libs.plugins.kotlin.jvm)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	api(project(":bouncer-api"))
	api(project(":bouncer-grpc-stubs"))

	api("io.quut:harmony-api:1.2.1")

	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
