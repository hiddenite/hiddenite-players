package eu.hiddenite.players.bungee.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import eu.hiddenite.players.bungee.BungeePlugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerEventsManager extends Manager {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private String tableName;

    public PlayerEventsManager(BungeePlugin plugin) {
        this.plugin = plugin;
        reload();

        plugin.getServer().getEventManager().register(plugin, this);
        plugin.getServer().getChannelRegistrar().register(MinecraftChannelIdentifier.from("hiddenite:afk"));
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().events.enabled;
        tableName = plugin.getConfig().events.table;

        plugin.getLogger().info("Player events are " + (isEnabled ? "enabled, table " + tableName : "disabled"));
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (!isEnabled) return;

        UUID playerId = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();
        String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();

        saveEvent("login", playerId, username, ip, null);
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        if (!isEnabled) return;

        UUID playerId = event.getPlayer().getUniqueId();
        String username = event.getPlayer().getUsername();

        saveEvent("logout", playerId, username, null, null);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (!isEnabled) return;

        Player player = event.getPlayer();
        // player.setPermission("hiddenite.afk", false);

        UUID playerId = player.getUniqueId();
        String username = player.getUsername();
        String serverName = event.getServer().getServerInfo().getName();

        saveEvent("server", playerId, username, null, serverName);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals("hiddenite:afk")) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!isEnabled) return;
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        boolean isAfk = event.getData()[0] != 0;

        String eventType = isAfk ? "+afk" : "-afk";
        saveEvent(eventType, player.getUniqueId(), player.getUsername(), null, null);
    }

    private void saveEvent(String eventType, UUID playerId, String username, String playerIp, String serverName) {
        OnlineTimeManager.getInstance().handlePlayerEvent(playerId, eventType, true);

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
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
        }).schedule();
    }
}
