package eu.hiddenite.players.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.hiddenite.players.velocity.commands.ReloadCommand;
import eu.hiddenite.players.velocity.commands.SameCommand;
import eu.hiddenite.players.velocity.managers.*;
import org.slf4j.Logger;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Plugin(id = "hiddenite-players",
        name = "HiddenitePlayers",
        version = "2.0.0",
        authors = { "Hiddenite" },
        dependencies = { @Dependency(id = "luckperms") }
)
public class VelocityPlugin {
    private final ProxyServer server;
    public ProxyServer getServer() {
        return server;
    }

    private final Logger logger;
    public Logger getLogger() {
        return logger;
    }

    private final Path dataDirectory;
    private Configuration config;
    public Configuration getConfig() {
        return config;
    }

    private Database database;
    public Database getDatabase() {
        return database;
    }

    private final ArrayList<Manager> managers = new ArrayList<>();

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public boolean reloadConfiguration() {
        Configuration config = loadConfiguration(dataDirectory.toFile());
        if (config == null) {
            return false;
        }
        this.config = config;

        for (Manager manager : managers) {
            manager.reload();
        }
        return true;
    }

    private Configuration loadConfiguration(File dataDirectory) {
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdir()) {
                logger.warn("Could not create the configuration folder.");
                return null;
            }
        }

        File file = new File(dataDirectory, "config.yml");
        if (!file.exists()) {
            logger.warn("No configuration file found, creating a default one.");

            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("proxy-config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        YamlConfigurationLoader reader = YamlConfigurationLoader.builder().path(dataDirectory.toPath().resolve("config.yml")).build();

        try {
            return reader.load().get(Configuration.class);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!reloadConfiguration()) {
            logger.warn("Invalid configuration, plugin not enabled.");
            return;
        }

        database = new Database(config, logger);
        if (!database.open()) {
            logger.warn("Could not connect to the database, plugin not enabled.");
            return;
        }

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("hiddenite:players:reload").plugin(this).build(),
                new ReloadCommand(this)
        );

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("same").plugin(this).build(),
                new SameCommand(this)
        );

        managers.add(new PlayersManager(this));
        managers.add(new CommandsFilterManager(this));
        managers.add(new RanksManager(this));
        managers.add(new OnlinePlayersManager(this));
        managers.add(new PlayerEventsManager(this));
        managers.add(new OnlineTimeManager(this));
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onProxyShutdown(ProxyShutdownEvent event) {
        for (Manager manager : managers) {
            manager.close();
        }
        database.close();
    }
}
