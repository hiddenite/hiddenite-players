package eu.hiddenite.players.velocity.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import eu.hiddenite.players.velocity.VelocityPlugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class OnlinePlayersManager extends Manager {
    private final VelocityPlugin plugin;

    private boolean isEnabled;
    private String tableName;
    private int updateInterval;
    private ScheduledTask task = null;

    private boolean shouldUpdateOnlinePlayers = true;

    public OnlinePlayersManager(VelocityPlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().onlinePlayers.enabled;
        if (!isEnabled) {
            if (task != null) {
                task.cancel();
                task = null;
            }
            return;
        }

        tableName = plugin.getConfig().onlinePlayers.table;

        int oldUpdateInterval = updateInterval;
        updateInterval = plugin.getConfig().onlinePlayers.updateInterval;

        if (oldUpdateInterval != updateInterval || task == null) {
            startTask();
        }
    }

    private void startTask() {
        if (task != null) {
            task.cancel();
        }

        task = plugin.getServer().getScheduler()
                .buildTask(plugin, this::updateOnlinePlayers)
                .repeat(updateInterval, TimeUnit.SECONDS)
                .schedule();
    }

    @Subscribe
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        shouldUpdateOnlinePlayers = true;
    }

    @Subscribe
    public void onPlayerDisconnectEvent(DisconnectEvent event) {
        shouldUpdateOnlinePlayers = true;
    }

    private void updateOnlinePlayers() {
        if (!shouldUpdateOnlinePlayers || !isEnabled) {
            return;
        }
        shouldUpdateOnlinePlayers = false;

        JsonArray jsonPlayers = new JsonArray();

        for (Player player : plugin.getServer().getAllPlayers()) {
            JsonObject jsonPlayer = new JsonObject();
            jsonPlayer.addProperty("name", player.getUsername());
            jsonPlayer.addProperty("uuid", player.getUniqueId().toString());
            jsonPlayers.add(jsonPlayer);
        }

        int playersCount = jsonPlayers.size();
        String playersData = jsonPlayers.toString();

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
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
        }).schedule();
    }
}
