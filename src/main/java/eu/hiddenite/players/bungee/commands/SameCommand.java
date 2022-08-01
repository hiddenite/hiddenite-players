package eu.hiddenite.players.bungee.commands;

import eu.hiddenite.players.bungee.BungeePlugin;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.net.InetSocketAddress;
import java.util.*;

public class SameCommand extends Command implements TabExecutor {
    private final BungeePlugin plugin;

    public SameCommand(BungeePlugin plugin) {
        super("same", "hiddenite.players.same");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponent("You must to specify a player."));
            return;
        }

        ProxiedPlayer target = plugin.getProxy().getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(new TextComponent("The player doesn't exist or isn't connected."));
            return;
        }

        String targetAddress = getHostAddress(target);
        List<String> playersWithSameIP = new ArrayList<>();

        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player != target && Objects.equals(getHostAddress(player), targetAddress)) {
                playersWithSameIP.add(player.getName());
            }
        }

        if (playersWithSameIP.size() < 1) {
            sender.sendMessage(new TextComponent("There is no connected user with the same IP as " + target.getName() + "."));
            return;
        }

        sender.sendMessage(new TextComponent("Connected users with the same IP as " + target.getName() + " :" + String.join(", ", playersWithSameIP)));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return matchPlayer(args[0]);
        }
        return new HashSet<>();
    }

    private String getHostAddress(ProxiedPlayer player) {
        if (player.getSocketAddress() instanceof InetSocketAddress address) {
            return address.getAddress().getHostAddress();
        }
        return null;
    }

    private Set<String> matchPlayer(String argument) {
        Set<String> matches = new HashSet<>();
        String search = argument.toUpperCase();
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.getName().toUpperCase().startsWith(search)) {
                matches.add(player.getName());
            }
        }
        return matches;
    }

}
