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

    public TablistListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Retrieve the header and footer lines from the configuration.
        List<String> headerLines = config.getStringList("header");
        List<String> footerLines = config.getStringList("footer");
        // Get the margin value; default is 0 if not set.
        int margin = config.getInt("margin", 0);

        // Process each line to add left/right spaces and a reset code.
        List<String> processedHeader = headerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());
        List<String> processedFooter = footerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());

        // Combine lines with newlines.
        String header = String.join("\n", processedHeader);
        String footer = String.join("\n", processedFooter);

        // Apply color codes.
        header = main.applyColors(header);
        footer = main.applyColors(footer);

        // Set the player's tablist header and footer.
        player.setPlayerListHeaderFooter(header, footer);
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
