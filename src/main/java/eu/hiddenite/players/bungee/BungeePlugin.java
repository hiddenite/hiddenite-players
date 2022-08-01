package eu.hiddenite.players.bungee;

import eu.hiddenite.players.Database;
import eu.hiddenite.players.bungee.commands.ReloadCommand;
import eu.hiddenite.players.bungee.commands.SameCommand;
import eu.hiddenite.players.bungee.managers.*;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

public class BungeePlugin extends Plugin {
    private Configuration config;
    public Configuration getConfig() {
        return config;
    }

    private Database database;
    public Database getDatabase() {
        return database;
    }

    private final ArrayList<Manager> managers = new ArrayList<>();

    @Override
    public void onEnable() {
        if (!reloadConfiguration()) {
            getLogger().warning("Invalid configuration, plugin not enabled.");
            return;
        }

        database = new Database(config, getLogger());
        if (!database.open()) {
            getLogger().warning("Could not connect to the database, plugin not enabled.");
            return;
        }

        getProxy().getPluginManager().registerCommand(this, new ReloadCommand(this));
        getProxy().getPluginManager().registerCommand(this, new SameCommand(this));

        managers.add(new PlayersManager(this));
        managers.add(new CommandsFilterManager(this));
        managers.add(new RanksManager(this));
        managers.add(new OnlinePlayersManager(this));
        managers.add(new PlayerEventsManager(this));
        managers.add(new OnlineTimeManager(this));
    }

    @Override
    public void onDisable() {
        database.close();
    }

    public boolean reloadConfiguration() {
        Configuration config = loadConfiguration();
        if (config == null) {
            return false;
        }
        this.config = config;

        for (Manager manager : managers) {
            manager.reload();
        }
        return true;
    }

    private Configuration loadConfiguration() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Could not create the configuration folder.");
                return null;
            }
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            getLogger().warning("No configuration file found, creating a default one.");

            try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        try {
            return ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
