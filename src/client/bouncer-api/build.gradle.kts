plugins {
	alias(libs.plugins.kotlin.jvm)
}

group = "io.quut"
version = "1.0-SNAPSHOT"

dependencies {
	compileOnlyApi(libs.adventure.api)
}
