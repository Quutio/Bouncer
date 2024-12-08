pluginManagement {
	repositories {
		gradlePluginPortal()
		maven {
			name = "sponge"
			url = uri("https://repo.spongepowered.org/repository/maven-public/")
		}
	}
	plugins {
		id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
		id("com.diffplug.spotless") version "6.25.0"
	}
}

rootProject.name = "bouncer"

include("bouncer-bukkit")
include("bouncer-velocity")
include("bouncer-common")
include("bouncer-grpc-stubs")
include("bouncer-protos")
include("bouncer-api")
include("bouncer-sponge")
