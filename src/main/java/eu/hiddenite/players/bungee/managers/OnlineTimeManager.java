package eu.hiddenite.players.bungee.managers;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.plugin.Listener;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;

public class OnlineTimeManager extends Manager implements Listener {
    private static class OnlineState {
        public long activeTime = 0;
        public long inactiveTime = 0;
        public boolean isActive = true;
        public long lastUpdate;

        private OnlineState(long now) {
            this.lastUpdate = now;
        }
    }

    private final BungeePlugin plugin;
    private final HashMap<UUID, OnlineState> playerStates = new HashMap<>();

    private boolean isEnabled;
    private String tableName;

    public OnlineTimeManager(BungeePlugin plugin) {
        this.plugin = plugin;
        instance = this;
        reload();
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("online-time.enabled");
        tableName = plugin.getConfig().getString("online-time.table");

        plugin.getLogger().info("Online time is " + (isEnabled ? "enabled, table " + tableName : "disabled"));
    }

    public void handlePlayerEvent(UUID playerId, String eventType) {
        long now = System.currentTimeMillis();

        if (eventType.equals("login")) {
            playerStates.put(playerId, new OnlineState(now));
            return;
        }

        OnlineState onlineState = playerStates.get(playerId);
        if (onlineState == null) {
            return;
        }

        long delta = now - onlineState.lastUpdate;
        onlineState.lastUpdate = now;

        if (eventType.equals("+afk") && onlineState.isActive) {
            onlineState.isActive = false;
            onlineState.activeTime += delta;
        }
        if ((eventType.equals("-afk") || eventType.equals("server")) && !onlineState.isActive) {
            onlineState.isActive = true;
            onlineState.inactiveTime += delta;
        }
        if (eventType.equals("logout")) {
            if (onlineState.isActive) {
                onlineState.activeTime += delta;
            } else {
                onlineState.inactiveTime += delta;
            }
            updateOnlineTime(playerId, onlineState.activeTime, onlineState.inactiveTime);
            playerStates.remove(playerId);
        }
    }

    private void updateOnlineTime(UUID playerId, long activeTime, long inactiveTime) {
        if (!isEnabled) return;

        plugin.getLogger().info("Adding online-time to " + playerId + ": "
                + activeTime + " active " + inactiveTime + " inactive");

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabase().prepareStatement(
                    "INSERT INTO `" + tableName + "`" +
                            " (player_uuid, active_time, inactive_time)" +
                            " VALUES (?, ?, ?)" +
                            " ON DUPLICATE KEY UPDATE active_time = active_time + ?," +
                            " inactive_time = inactive_time + ?"
            )) {
                ps.setString(1, playerId.toString());
                ps.setLong(2, activeTime);
                ps.setLong(3, inactiveTime);
                ps.setLong(4, activeTime);
                ps.setLong(5, inactiveTime);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private static OnlineTimeManager instance;
    public static OnlineTimeManager getInstance() {
        return instance;
    }
}
