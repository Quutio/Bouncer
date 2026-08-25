package io.quut.bouncer.omnivisor.sponge.util

import io.quut.bouncer.omnivisor.sponge.BouncerMultiverseConfig
import net.kyori.option.Option
import net.kyori.option.OptionSchema
import net.kyori.option.OptionState
import java.net.InetSocketAddress

internal object Options
{
	internal object Holder
	{
		internal val SCHEMA: OptionSchema = Options.builder.frozenView()
	}

	private val builder: OptionSchema.Mutable = OptionSchema.emptySchema()

	internal val NAME: Option<String> = this.builder.stringOption("name", null)
	internal val GROUP: Option<String> = this.builder.stringOption("group", null)
	internal val TYPE: Option<String> = this.builder.stringOption("type", null)

	internal val HOST: Option<String> = this.builder.stringOption("host", null)
	internal val PORT: Option<Int> = this.builder.intOption("port", 0)

	internal fun loadConfig(options: OptionState): BouncerMultiverseConfig =
		BouncerMultiverseConfig(
			options.value(this.NAME) ?: throw IllegalArgumentException("Missing name"),
			options.value(this.GROUP) ?: throw IllegalArgumentException("Missing group"),
			options.value(this.TYPE) ?: throw IllegalArgumentException("Missing type"),
			this.loadAddress(options))

	private fun loadAddress(options: OptionState): InetSocketAddress?
	{
		if (!options.has(this.HOST) || !options.has(this.PORT))
		{
			return null
		}

		return InetSocketAddress.createUnresolved(options.value(this.HOST), options.value(this.PORT)!!)
	}
}
