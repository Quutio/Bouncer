plugins {
	alias(libs.plugins.kotlin.jvm)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	api(project(":bouncer-api"))
	api(project(":bouncer-grpc-stubs"))

	implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
