package eu.hiddenite.players.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventLoggerManager implements Listener {
    private final BukkitPlugin plugin;

    public EventLoggerManager(BukkitPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Player " + player.getName() + " joined" +
                ", location " + player.getLocation() +
                ", health " + player.getHealth() +
                ", mode " + player.getGameMode());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getLogger().info("Player " + player.getName() + " left" +
                ", location " + player.getLocation() +
                ", health " + player.getHealth() +
                ", mode " + player.getGameMode());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        plugin.getLogger().info("Player " + player.getName() + " died" +
                ", location " + player.getLocation() +
                ", drops " + event.getDrops());
    }
}
