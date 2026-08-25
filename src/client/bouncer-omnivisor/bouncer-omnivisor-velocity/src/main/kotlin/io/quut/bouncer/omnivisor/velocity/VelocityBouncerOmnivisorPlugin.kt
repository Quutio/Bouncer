package io.quut.bouncer.omnivisor.velocity

import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin

@Plugin(
	id = "bouncer_omnivisor",
	name = "Bouncer Omnivisor",
	version = "1.0-SNAPSHOT",
	authors = ["Joni Aromaa (isokissa3)"],
	dependencies = [Dependency("bouncer"), Dependency("omnivisor")])
class VelocityBouncerOmnivisorPlugin
