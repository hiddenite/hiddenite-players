package eu.hiddenite.perms.bungee;

import eu.hiddenite.perms.Database;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PermsBungeePlugin extends Plugin implements Listener {
    private Configuration config;
    private Database database;

    private final HashMap<Integer, List<String>> rankPermissions = new HashMap<>();
    private final HashSet<String> allowedCommands = new HashSet<>();
    private String commandNotAllowedMessage;

    @Override
    public void onEnable() {
        if (!loadConfiguration()) {
            return;
        }

        database = new Database(config, getLogger());
        if (!database.open()) {
            getLogger().warning("Could not connect to the database. Plugin disabled.");
            return;
        }

        loadRankPermissions();
        allowedCommands.addAll(config.getStringList("allowed-commands"));
        commandNotAllowedMessage = config.getString("command-not-allowed-message");

        getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void onDisable() {
        database.close();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent event) {
        if (event.isCancelled()) {
            return;
        }

        UUID playerId = event.getConnection().getUniqueId();
        String playerName = event.getConnection().getName();

        try (PreparedStatement ps = database.prepareStatement("INSERT INTO players" +
                " (id, username)" +
                " VALUES (?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                " last_login = NOW(), username = ?")) {
            ps.setString(1, playerId.toString());
            ps.setString(2, playerName);
            ps.setString(3, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

        int playerRank = 0;
        try (PreparedStatement ps = database.prepareStatement("SELECT rank FROM players WHERE id = ?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    playerRank = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (rankPermissions.containsKey(playerRank)) {
            List<String> permissions = rankPermissions.get(playerRank);
            for (String permission : permissions) {
                event.getPlayer().setPermission(permission, true);
            }
            getLogger().info("Player " + playerName + " has rank " + playerRank + ", added " + permissions.size() + " permissions.");
        } else if (playerRank > 0) {
            getLogger().warning("Player " + playerName + " has rank " + playerRank + ", but this rank has no permission.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        if (!event.getMessage().startsWith("/")) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer)event.getSender();
        if (player.hasPermission("hiddenite.perms.bypass")) {
            return;
        }

        String command = event.getMessage().split(" ")[0].substring(1);
        String search = command.toLowerCase();

        if (!allowedCommands.contains(search)) {
            String errorMessage = commandNotAllowedMessage.replace("{COMMAND}", command);
            player.sendMessage(TextComponent.fromLegacyText(errorMessage));
            event.setCancelled(true);
        }
    }

    private boolean loadConfiguration() {
        if (!getDataFolder().exists()) {
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Could not create the configuration folder.");
                return false;
            }
        }

        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            getLogger().warning("No configuration file found, creating a default one.");

            try (InputStream in = getResourceAsStream("bungee-config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            config = ConfigurationProvider
                    .getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void loadRankPermissions() {
        Configuration rankSection = config.getSection("rank-permissions");
        for (String key : rankSection.getKeys()) {
            rankPermissions.put(Integer.parseInt(key), rankSection.getStringList(key));
        }

        getLogger().info("Loaded " + rankPermissions.size() + " ranks from the configuration");
    }
}
