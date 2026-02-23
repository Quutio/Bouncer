package io.quut.bouncer.sponge

import com.google.inject.Inject
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import java.lang.invoke.MethodHandles

internal class SpongeBouncerPluginInfo @Inject constructor(
	@param: DefaultConfig(sharedRoot = false) val configLoader: ConfigurationLoader<CommentedConfigurationNode>)
{
	val lookup: MethodHandles.Lookup = MethodHandles.lookup()
}
