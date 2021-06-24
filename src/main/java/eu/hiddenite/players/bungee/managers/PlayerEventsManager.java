package eu.hiddenite.players.bungee.managers;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.InetSocketAddress;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerEventsManager extends Manager implements Listener {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private String tableName;

    public PlayerEventsManager(BungeePlugin plugin) {
        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);

        reload();

        plugin.getProxy().registerChannel("hiddenite:afk");
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("events.enabled");
        tableName = plugin.getConfig().getString("events.table");

        plugin.getLogger().info("Player events are " + (isEnabled ? "enabled, table " + tableName : "disabled"));
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        if (!isEnabled) return;

        UUID playerId = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();

        String ip = null;
        if (event.getPlayer().getSocketAddress() instanceof InetSocketAddress address) {
            ip = address.getAddress().getHostAddress();
        }

        saveEvent("login", playerId, username, ip, null);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (!isEnabled) return;

        UUID playerId = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getName();
        saveEvent("logout", playerId, username, null, null);
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        if (!isEnabled) return;

        ProxiedPlayer player = event.getPlayer();
        player.setPermission("hiddenite.afk", false);

        UUID playerId = player.getUniqueId();
        String username = player.getName();
        String serverName = event.getServer().getInfo().getName();
        saveEvent("server", playerId, username, null, serverName);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals("hiddenite:afk")) {
            return;
        }
        event.setCancelled(true);

        if (!isEnabled) return;
        if (!(event.getReceiver() instanceof ProxiedPlayer player)) {
            return;
        }

        boolean isAfk = event.getData()[0] != 0;
        player.setPermission("hiddenite.afk", isAfk);

        String eventType = isAfk ? "+afk" : "-afk";
        saveEvent(eventType, player.getUniqueId(), player.getName(), null, null);
    }

    private void saveEvent(String eventType, UUID playerId, String username, String playerIp, String serverName) {
        OnlineTimeManager.getInstance().handlePlayerEvent(playerId, eventType);

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (PreparedStatement ps = plugin.getDatabase().prepareStatement(
                    "INSERT INTO `" + tableName + "`" +
                            " (event_type, player_uuid, username, player_ip, server_name)" +
                            " VALUES (?, ?, ?, ?, ?)"
            )) {
                ps.setString(1, eventType);
                ps.setString(2, playerId.toString());
                ps.setString(3, username);
                ps.setString(4, playerIp);
                ps.setString(5, serverName);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}
