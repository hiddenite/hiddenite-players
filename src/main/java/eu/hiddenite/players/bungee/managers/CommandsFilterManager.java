package eu.hiddenite.players.bungee.managers;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.HashSet;

public class CommandsFilterManager extends Manager implements Listener {
    private final BungeePlugin plugin;

    private boolean isEnabled;
    private HashSet<String> allowedCommands;
    private String commandNotAllowedMessage;

    public CommandsFilterManager(BungeePlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().getBoolean("commands-filter.enabled");
        allowedCommands = new HashSet<>();
        allowedCommands.addAll(plugin.getConfig().getStringList("commands-filter.allowed-commands"));
        commandNotAllowedMessage = plugin.getConfig().getString("commands-filter.not-allowed-message");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(ChatEvent event) {
        if (event.isCancelled() || !isEnabled) {
            return;
        }
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        if (!event.getMessage().startsWith("/")) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer)event.getSender();
        if (player.hasPermission("hiddenite.perms.bypass")) {
            return;
        }

        String command = event.getMessage().split(" ")[0].substring(1);
        String search = command.toLowerCase();

        if (!allowedCommands.contains(search)) {
            String errorMessage = commandNotAllowedMessage.replace("{COMMAND}", command);
            player.sendMessage(TextComponent.fromLegacyText(errorMessage));
            event.setCancelled(true);
        }
    }
}
