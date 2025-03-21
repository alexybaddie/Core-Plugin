package net.lexibaddie.modules.listeners;

import net.lexibaddie.core.main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.stream.Collectors;

public class TablistListener implements Listener {

    private final YamlConfiguration config;
    private final main plugin; // using the plugin instance

    public TablistListener(YamlConfiguration config, main plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Retrieve header and footer lines from the config.
        List<String> headerLines = config.getStringList("header");
        List<String> footerLines = config.getStringList("footer");
        int margin = config.getInt("margin", 0);

        // Process each line: add left/right margin spaces and append a reset code.
        List<String> processedHeader = headerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());
        List<String> processedFooter = footerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());

        String header = plugin.applyColors(String.join("\n", processedHeader));
        String footer = plugin.applyColors(String.join("\n", processedFooter));

        // Set the player's tablist header and footer.
        player.setPlayerListHeaderFooter(header, footer);

        // If smallcaps option is enabled, update the player's tab list name using the plugin's toSmallCaps.
        if (config.getBoolean("smallcaps", false)) {
            player.setPlayerListName(plugin.toSmallCaps(player.getDisplayName()));
        }
    }

    // Helper method to process a line by adding margin spaces and a reset code.
    private String processLine(String line, int margin) {
        String marginSpaces = getMarginSpaces(margin);
        return marginSpaces + line + marginSpaces + "&r";
    }

    // Helper method to build a string with 'margin' spaces.
    private String getMarginSpaces(int margin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < margin; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
