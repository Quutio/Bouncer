plugins {
	alias(libs.plugins.kotlin.jvm)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	api(project(":bouncer-api"))
	api(project(":bouncer-grpc-stubs"))
}
