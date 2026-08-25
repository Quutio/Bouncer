plugins {
	alias(libs.plugins.kotlin.jvm)
}

dependencies {
	api(project(":bouncer-api"))
	api(project(":bouncer-grpc-stubs"))

	implementation(libs.caffeine) {
		exclude("org.checkerframework", "checker-qual")
		exclude("com.google.errorprone", "error_prone_annotations")
	}
}
