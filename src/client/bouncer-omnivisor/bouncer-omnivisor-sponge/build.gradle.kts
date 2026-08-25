import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.sponge)
}

dependencies {
	compileOnly(project(":bouncer-api"))

	compileOnly(libs.omnivisor.api)
}

sponge {
	apiVersion("17.0.0")
	license("MIT")
	loader {
		name(PluginLoaders.JAVA_PLAIN)
		version("1.0")
	}
	plugin("bouncer_omnivisor") {
		entrypoint("io.quut.bouncer.omnivisor.sponge.SpongeBouncerOmnivisorPluginLoader")
		displayName("Bouncer Omnivisor")
		description("A load balancer for Minecraft servers")
		links {
			homepage("https://github.com/Quutio/Bouncer")
			source("https://github.com/Quutio/Bouncer")
			issues("https://github.com/Quutio/Bouncer/issues")
		}
		contributor("Joni Aromaa (isokissa3)") {
			description("Lead Developer")
		}
		contributor("Matias Paavilainen (Masa)") {
			description("Lead Developer")
		}
		dependency("spongeapi") {
			loadOrder(PluginDependency.LoadOrder.AFTER)
			optional(false)
		}
		dependency("bouncer") {
			loadOrder(PluginDependency.LoadOrder.AFTER)
			version("1.0-SNAPSHOT")
			optional(false)
		}
		dependency("omnivisor") {
			loadOrder(PluginDependency.LoadOrder.AFTER)
			version("0.1-SNAPSHOT")
			optional(false)
		}
	}
}
