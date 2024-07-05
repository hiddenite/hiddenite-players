package eu.hiddenite.players.velocity.managers;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.players.velocity.VelocityPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashSet;

public class CommandsFilterManager extends Manager {
    private final VelocityPlugin plugin;

    private boolean isEnabled;
    private HashSet<String> allowedCommands;
    private String commandNotAllowedMessage;

    public CommandsFilterManager(VelocityPlugin plugin) {
        this.plugin = plugin;
        reload();
        plugin.getServer().getEventManager().register(plugin, this);
    }

    @Override
    public void reload() {
        isEnabled = plugin.getConfig().commandsFilter.enabled;
        allowedCommands = new HashSet<>();
        allowedCommands.addAll(plugin.getConfig().commandsFilter.allowedCommands);
        commandNotAllowedMessage = plugin.getConfig().commandsFilter.notAllowedMessage;
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!event.getResult().isAllowed() || !isEnabled) {
            return;
        }
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }
        if (player.hasPermission("hiddenite.perms.bypass")) {
            return;
        }

        String command = event.getCommand().split(" ")[0];
        String search = command.toLowerCase();

        if (!allowedCommands.contains(search)) {
            String errorMessage = commandNotAllowedMessage.replace("{COMMAND}", command);
            player.sendMessage(MiniMessage.miniMessage().deserialize(errorMessage));
            event.setResult(CommandExecuteEvent.CommandResult.denied());
        }
    }
}
