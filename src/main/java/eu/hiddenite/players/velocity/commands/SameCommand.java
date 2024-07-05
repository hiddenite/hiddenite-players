package eu.hiddenite.players.velocity.commands;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import eu.hiddenite.players.velocity.VelocityPlugin;
import net.kyori.adventure.text.Component;

import java.util.*;

public class SameCommand implements SimpleCommand {
    private final VelocityPlugin plugin;

    public SameCommand(VelocityPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("hiddenite.players.same");
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("You must to specify a player."));
            return;
        }

        Player target = plugin.getServer().getPlayer(args[0]).orElse(null);
        if (target == null) {
            source.sendMessage(Component.text("The player doesn't exist or isn't connected."));
            return;
        }

        String targetAddress = getHostAddress(target);
        List<String> playersWithSameIP = new ArrayList<>();

        for (Player player : plugin.getServer().getAllPlayers()) {
            if (player != target && Objects.equals(getHostAddress(player), targetAddress)) {
                playersWithSameIP.add(player.getUsername());
            }
        }

        if (playersWithSameIP.size() < 1) {
            source.sendMessage(Component.text("There is no connected user with the same IP as " + target.getUsername() + "."));
            return;
        }

        source.sendMessage(Component.text("Connected users with the same IP as " + target.getUsername() + " :" + String.join(", ", playersWithSameIP)));
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return matchPlayer(args[0]);
        }
        return ImmutableList.of();
    }

    private String getHostAddress(Player player) {
        return player.getRemoteAddress().getAddress().getHostAddress();
    }

    private List<String> matchPlayer(String argument) {
        List<String> matches = new ArrayList<>();
        String search = argument.toUpperCase();
        for (Player player : plugin.getServer().getAllPlayers()) {
            if (player.getUsername().toUpperCase().startsWith(search)) {
                matches.add(player.getUsername());
            }
        }
        return matches;
    }

}
