package eu.hiddenite.perms.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;

public class PermsBukkitPlugin extends JavaPlugin implements Listener {
    private final HashSet<String> blockedCommands = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        blockedCommands.addAll(getConfig().getStringList("blocked-commands"));

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerCommandSendEvent(PlayerCommandSendEvent event) {
        if (event.getPlayer().hasPermission("hiddenite.perms.bypass")) {
            return;
        }

        event.getCommands().removeIf((command) -> {
            String search = command.toLowerCase();
            if (command.contains(":") || blockedCommands.contains(search)) {
                return true;
            }
            return false;
        });
    }
}
