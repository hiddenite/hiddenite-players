package eu.hiddenite.players.bungee.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import eu.hiddenite.players.bungee.BungeePlugin;
import net.kyori.adventure.text.Component;

public class ReloadCommand implements SimpleCommand {
    private final BungeePlugin plugin;

    public ReloadCommand(BungeePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.players.reload");
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();

        if (plugin.reloadConfiguration()) {
            source.sendMessage(Component.text("Reloaded successfully."));
        } else {
            source.sendMessage(Component.text("Could not reload the configuration."));
        }
    }
}
