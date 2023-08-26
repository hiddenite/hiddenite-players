/*
package eu.hiddenite.players.bungee.managers;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RanksManager extends Manager implements Listener {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private String tableName;
    private String idFieldName;
    private HashMap<Integer, List<String>> rankPermissions;

    public RanksManager(BungeePlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("ranks.enabled");
        tableName = plugin.getConfig().getString("ranks.table");
        idFieldName = plugin.getConfig().getString("ranks.field-id", "id");
        loadRankPermissions();
    }

    private void loadRankPermissions() {
        rankPermissions = new HashMap<>();

        Configuration rankSection = plugin.getConfig().getSection("ranks.permissions");
        for (String key : rankSection.getKeys()) {
            rankPermissions.put(Integer.parseInt(key), rankSection.getStringList(key));
        }

        plugin.getLogger().info("Loaded " + rankPermissions.size() + " ranks from the configuration");
    }

    @EventHandler
    public void onPlayerLogin(PostLoginEvent event) {
        if (!isEnabled) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();

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

        if (rankPermissions.containsKey(playerRank)) {
            List<String> permissions = rankPermissions.get(playerRank);
            for (String permission : permissions) {
                event.getPlayer().setPermission(permission, true);
            }
            plugin.getLogger().info("Player " + playerName + " has rank " + playerRank + ", added " + permissions.size() + " permissions.");
        } else if (playerRank > 0) {
            plugin.getLogger().warning("Player " + playerName + " has rank " + playerRank + ", but this rank has no permission.");
        }
    }
}
*/