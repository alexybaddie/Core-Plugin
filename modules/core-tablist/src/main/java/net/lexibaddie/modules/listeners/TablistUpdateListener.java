package net.lexibaddie.modules.listeners;

import net.lexibaddie.core.main;
import net.lexibaddie.modules.events.TablistUpdateEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.stream.Collectors;

public class TablistUpdateListener implements Listener {

    private final YamlConfiguration config;
    private final net.lexibaddie.core.main plugin;

    public TablistUpdateListener(YamlConfiguration config, net.lexibaddie.core.main plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @EventHandler
    public void onTablistUpdate(TablistUpdateEvent event) {
        // Retrieve header and footer lines from the config.
        List<String> headerLines = config.getStringList("header");
        List<String> footerLines = config.getStringList("footer");
        int margin = config.getInt("margin", 0);

        List<String> processedHeader = headerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());
        List<String> processedFooter = footerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());

        String header = String.join("\n", processedHeader);
        String footer = String.join("\n", processedFooter);

        header = main.applyColors(header);
        footer = main.applyColors(footer);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(header, footer);
        }
    }

    // Helper method to generate a string with 'margin' spaces.
    private String getMarginSpaces(int margin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < margin; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    // Helper method to process a line by adding margin spaces on left/right and appending a reset code.
    private String processLine(String line, int margin) {
        String marginSpaces = getMarginSpaces(margin);
        return marginSpaces + line + marginSpaces + "&r";
    }

}
