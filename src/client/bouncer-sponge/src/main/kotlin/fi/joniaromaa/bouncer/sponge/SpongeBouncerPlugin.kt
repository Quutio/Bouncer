package fi.joniaromaa.bouncer.sponge;

import com.google.inject.Inject
import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.api.server.IBouncerServer
import fi.joniaromaa.bouncer.common.BouncerAPI
import fi.joniaromaa.bouncer.sponge.config.PluginConfig
import fi.joniaromaa.bouncer.sponge.listeners.PlayerListener
import org.apache.logging.log4j.Logger
import org.spongepowered.api.Server
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.io.File
import java.lang.Exception
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.*

@Plugin("bouncer")
public class SpongeBouncerPlugin
@Inject constructor(
    private val container: PluginContainer,
    private val logger: Logger
) {

    @Inject
    @ConfigDir(sharedRoot = false)
    private lateinit var configDir: Path

    private lateinit var config: PluginConfig;

    private lateinit var bouncer: BouncerAPI;
    private lateinit var bouncerServer: IBouncerServer;

    @Listener
    fun onEnable(event: StartedEngineEvent<Server>) {
        this.loadPluginConfig();

        val optAddress: Optional<InetSocketAddress> = Sponge.server().boundAddress();

        val address: String = if (optAddress.isPresent) optAddress.get().address.hostAddress else "";

        println("Address: $address");

        val ip = DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName("1.1.1.1"), 53)
            return@use socket.localAddress.hostAddress;
        };

        if(ip.isEmpty()) {
            throw Exception("Address error")
        }

        val info = BouncerServerInfo(
            this.config.name,
            this.config.group,
            this.config.type,



            InetSocketAddress.createUnresolved(
                ip,
                optAddress.get().port
            )
        )

        this.bouncer = BouncerAPI(this.config.apiUrl);
        this.bouncerServer = this.bouncer.serverLoadBalancer.registerServer(info);

        for (player: ServerPlayer in Sponge.server().onlinePlayers()) {
            this.bouncerServer.confirmJoin(player.uniqueId())
        }

        Sponge.eventManager().registerListeners(this.container, PlayerListener(this.bouncerServer))
    }

    private fun loadPluginConfig() {
        val loader: HoconConfigurationLoader =
            HoconConfigurationLoader.builder()
                .file(File(this.configDir.toFile(), "config.conf"))
                .defaultOptions {
                    it.shouldCopyDefaults(true)
                }.build()

        val node: ConfigurationNode = loader.load();
        this.config = PluginConfig.loadFrom(node);
        this.config.saveTo(node);
        loader.save(node);
    }

    companion object
    {
        @JvmStatic
        internal lateinit var container: PluginContainer;
    }

}
