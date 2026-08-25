plugins {
	alias(libs.plugins.kotlin.jvm)
}

dependencies {
	compileOnlyApi(libs.adventure.api)
	compileOnlyApi(libs.fusion.api)
}
