package eu.hiddenite.players.bungee.managers;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayersManager extends Manager implements Listener {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private String tableName;

    public PlayersManager(BungeePlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("players.enabled");
        tableName = plugin.getConfig().getString("players.table");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent event) {
        if (event.isCancelled() || !isEnabled) {
            return;
        }

        UUID playerId = event.getConnection().getUniqueId();
        String playerName = event.getConnection().getName();

        try (PreparedStatement ps = plugin.getDatabase().prepareStatement(
                "INSERT INTO `" + tableName + "`" +
                " (id, username)" +
                " VALUES (?, ?)" +
                " ON DUPLICATE KEY UPDATE" +
                " last_login = NOW(), username = ?"
        )) {
            ps.setString(1, playerId.toString());
            ps.setString(2, playerName);
            ps.setString(3, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
