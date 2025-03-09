package net.lexibaddie.modules;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatChannelManager {
    // Use a thread-safe map.
    private static Map<Player, String> playerChannels = new ConcurrentHashMap<>();
    private static String defaultChannel = "all";

    public static void init(YamlConfiguration config) {
        if (config.contains("default")) {
            defaultChannel = config.getString("default");
        }
    }

    public static void setChannel(Player player, String channel) {
        playerChannels.put(player, channel);
        // Debug log.
    //    System.out.println("Channel for " + player.getName() + " set to " + channel);
    }

    public static String getChannel(Player player) {
        return playerChannels.getOrDefault(player, defaultChannel);
    }

    public static void removePlayer(Player player) {
        playerChannels.remove(player);
    }
}
