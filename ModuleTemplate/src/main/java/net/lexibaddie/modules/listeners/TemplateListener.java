package net.lexibaddie.modules.listeners;

import net.lexibaddie.core.main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TemplateListener implements Listener {

    private final YamlConfiguration config;

    public TemplateListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Retrieve the join message from the config and send it to the player.
        String joinMessage = config.getString("join-message", "&aWelcome to our server!");
        event.getPlayer().sendMessage(main.applyColors(joinMessage));
    }
}
