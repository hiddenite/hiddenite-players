package eu.hiddenite.players.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class BukkitPlugin extends JavaPlugin implements Listener {
    private final HashSet<String> blockedCommands = new HashSet<>();
    private int inactivityTime;

    private final HashMap<UUID, Long> lastActivity = new HashMap<>();
    private final HashSet<UUID> inactivePlayers = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        blockedCommands.addAll(getConfig().getStringList("blocked-commands"));
        inactivityTime = getConfig().getInt("inactivity-time", 60);

        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::checkAfkPlayers, 30, 30);
        getServer().getMessenger().registerOutgoingPluginChannel( this, "hiddenite:afk");

        new EventLoggerManager(this);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @EventHandler
    public void onPlayerCommandSendEvent(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission("hiddenite.perms.bypass")) {
            return;
        }

        event.getCommands().removeIf((command) -> {
            String search = command.toLowerCase();
            return command.contains(":") || blockedCommands.contains(search);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setNoDamageTicks(0);

        inactivePlayers.remove(event.getPlayer().getUniqueId());
        lastActivity.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        inactivePlayers.remove(event.getPlayer().getUniqueId());
    }

    private void updateActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (inactivePlayers.contains(player.getUniqueId())) {
            player.sendPluginMessage(this, "hiddenite:afk", new byte[] { 0 });
            inactivePlayers.remove(player.getUniqueId());
        }
    }

    private void checkActivity(Player player, long now) {
        if (inactivePlayers.contains(player.getUniqueId())) {
            return;
        }
        if (!lastActivity.containsKey(player.getUniqueId())) {
            return;
        }
        long delta = now - lastActivity.get(player.getUniqueId());
        if (delta > inactivityTime * 1000L) {
            player.sendPluginMessage(this, "hiddenite:afk", new byte[] { 1 });
            inactivePlayers.add(player.getUniqueId());
        }
    }

    private void checkAfkPlayers() {
        long now = System.currentTimeMillis();
        for (Player player : getServer().getOnlinePlayers()) {
            checkActivity(player, now);
        }
    }
}
