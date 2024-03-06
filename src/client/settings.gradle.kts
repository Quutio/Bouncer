pluginManagement {
	plugins {
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
