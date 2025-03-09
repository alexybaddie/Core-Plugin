package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EventListener implements Listener {

    private final YamlConfiguration config;

    public EventListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (config.getBoolean("messaging.enabled", false)) {
            String chatFormat = main.applyColors(
                    config.getString("messaging.chat.chat_format", "%secondary%%player% %primary%» %secondary%%message%")
            );
            event.setFormat(chatFormat
                    .replace("%player%", event.getPlayer().getDisplayName())
                    .replace("%message%", event.getMessage()));
        }
    }
}
