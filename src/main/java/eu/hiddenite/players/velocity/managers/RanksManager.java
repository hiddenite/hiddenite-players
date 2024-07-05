package eu.hiddenite.players.velocity.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.players.velocity.VelocityPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class RanksManager extends Manager {
    private final VelocityPlugin plugin;
    private final LuckPerms luckPerms;

    public RanksManager(VelocityPlugin plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
        reload();
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void reload() {
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        if (!plugin.getConfig().ranks.enabled) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String playerName = player.getUsername();

        String tableName = plugin.getConfig().ranks.table;
        String idFieldName = plugin.getConfig().ranks.fieldId;

        int playerRank = 0;
        try (PreparedStatement ps = plugin.getDatabase().prepareStatement(
                "SELECT rank FROM `" + tableName + "` WHERE `" + idFieldName + "` = ?"
        )) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    playerRank = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for (var entry : plugin.getConfig().ranks.groups.entrySet()) {
            int rank = entry.getKey();
            String group = entry.getValue();
            if (playerRank == rank && !player.hasPermission("group." + group)) {
                plugin.getLogger().info("Adding group " + group + " to player " + playerName);
                luckPerms.getUserManager().modifyUser(playerId, user -> {
                    user.data().add(Node.builder("group." + group).build());
                });
            }
            if (playerRank != rank && player.hasPermission("group." + group)) {
                plugin.getLogger().info("Removing group " + group + " from player " + playerName);
                luckPerms.getUserManager().modifyUser(playerId, user -> {
                    user.data().remove(Node.builder("group." + group).build());
                });
            }
        }
    }
}
