package eu.hiddenite.players.bungee.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class OnlinePlayersManager extends Manager implements Listener {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private String tableName;
    private int updateInterval;
    private ScheduledTask task = null;

    private boolean shouldUpdateOnlinePlayers = true;

    public OnlinePlayersManager(BungeePlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("online-players.enabled");
        if (!isEnabled) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            return;
        }

        tableName = plugin.getConfig().getString("online-players.table");

        int oldUpdateInterval = updateInterval;
        updateInterval = plugin.getConfig().getInt("online-players.update-interval");

        if (oldUpdateInterval != updateInterval || task == null) {
            startTask();
        }
    }

    private void startTask() {
        if (task != null) {
            task.cancel();
        }
        task = plugin.getProxy().getScheduler().schedule(plugin,
                this::updateOnlinePlayers, 1,
                updateInterval,
                TimeUnit.SECONDS
        );
    }

    @EventHandler
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        shouldUpdateOnlinePlayers = true;
    }

    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent event) {
        shouldUpdateOnlinePlayers = true;
    }

    private void updateOnlinePlayers() {
        if (!shouldUpdateOnlinePlayers || !isEnabled) {
            return;
        }
        shouldUpdateOnlinePlayers = false;

        JsonArray jsonPlayers = new JsonArray();

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            JsonObject jsonPlayer = new JsonObject();
            jsonPlayer.addProperty("name", player.getName());
            jsonPlayer.addProperty("uuid", player.getUniqueId().toString());
            jsonPlayers.add(jsonPlayer);
        }

        int playersCount = jsonPlayers.size();
        String playersData = jsonPlayers.toString();

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                try (PreparedStatement ps = plugin.getDatabase().prepareStatement(
                        "INSERT INTO `" + tableName + "`" +
                        " (creation_date, players_count, players_data)" +
                        " VALUES (NOW(), ?, ?)"
                )) {
                    ps.setInt(1, playersCount);
                    ps.setString(2, playersData);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
