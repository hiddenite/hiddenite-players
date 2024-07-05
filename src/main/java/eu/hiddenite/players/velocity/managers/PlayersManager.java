package eu.hiddenite.players.velocity.managers;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import eu.hiddenite.players.velocity.VelocityPlugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class PlayersManager extends Manager {
    private final VelocityPlugin plugin;

    private boolean isEnabled;
    private String tableName;

    public PlayersManager(VelocityPlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().players.enabled;
        tableName = plugin.getConfig().players.table;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerLogin(LoginEvent event) {
        if (!event.getResult().isAllowed() || !isEnabled) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getUsername();

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
