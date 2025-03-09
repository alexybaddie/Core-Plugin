package net.lexibaddie.modules.listeners;

import net.lexibaddie.core.main;
import net.lexibaddie.modules.ChatChannelManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatChannelListener implements Listener {

    private final YamlConfiguration config;

    public ChatChannelListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String channel = ChatChannelManager.getChannel(sender);

        // Debug log: print out which channel the sender is in.
    //    Bukkit.getLogger().info(sender.getName() + " is in channel: " + channel);

        // For the default "all" channel, let normal chat occur.
        if (channel.equalsIgnoreCase("all")) {
            return;
        }

        // Cancel the default event so we can re‑broadcast.
        event.setCancelled(true);

        // Get the channel-specific format. If missing, fallback.
        String format = config.getString("channels." + channel + ".format");
        if (format == null || format.trim().isEmpty()) {
            format = "%player% » %message%";
        }

        String formatted = main.applyColors(format)
                .replace("%player%", sender.getDisplayName())
                .replace("%message%", event.getMessage());

        // Get the permission required for the channel.
        String permission = config.getString("channels." + channel + ".permission", "");
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (permission.isEmpty() || p.hasPermission(permission)) {
                p.sendMessage(formatted);
                count++;
            }
        }
    //    Bukkit.getLogger().info("Broadcasted message from " + sender.getName() + " in channel '" + channel + "' to " + count + " recipients.");
    }
}
