package eu.hiddenite.players.velocity;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.List;

@ConfigSerializable
public class Configuration {
    public MySQL mysql;
    public Players players;
    public Events events;
    public OnlineTime onlineTime;
    public CommandsFilter commandsFilter;
    public OnlinePlayers onlinePlayers;
    public Ranks ranks;

    @ConfigSerializable
    public static class MySQL {
        public String host;
        public String user;
        public String password;
        public String database;
    }

    @ConfigSerializable
    public static class Players {
        public boolean enabled;
        public String table;
    }

    @ConfigSerializable
    public static class Events {
        public boolean enabled;
        public String table;
    }

    @ConfigSerializable
    public static class OnlineTime {
        public boolean enabled;
        public String table;
    }

    @ConfigSerializable
    public static class CommandsFilter {
        public boolean enabled;
        public List<String> allowedCommands;
        public String notAllowedMessage;
    }

    @ConfigSerializable
    public static class OnlinePlayers {
        public boolean enabled;
        public String table;
        public int updateInterval;
    }

    @ConfigSerializable
    public static class Ranks {
        public boolean enabled;
        public String table;
        public String fieldId = "id";
        public HashMap<Integer, String> groups;
    }
}
