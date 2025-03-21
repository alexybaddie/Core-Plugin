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
    private final main plugin;

    public TablistUpdateListener(YamlConfiguration config, net.lexibaddie.core.main plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @EventHandler
    public void onTablistUpdate(TablistUpdateEvent event) {
        List<String> headerLines = config.getStringList("header");
        List<String> footerLines = config.getStringList("footer");
        int margin = config.getInt("margin", 0);

        List<String> processedHeader = headerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());
        List<String> processedFooter = footerLines.stream()
                .map(line -> processLine(line, margin))
                .collect(Collectors.toList());

        String header = main.applyColors(String.join("\n", processedHeader));
        String footer = main.applyColors(String.join("\n", processedFooter));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.setPlayerListHeaderFooter(header, footer);
            // Update player's tab list name if the option is enabled.
            if (config.getBoolean("smallcaps", false)) {
                player.setPlayerListName(plugin.toSmallCaps(player.getName()));
            }
        }
    }

    private String processLine(String line, int margin) {
        String marginSpaces = getMarginSpaces(margin);
        return marginSpaces + line + marginSpaces + "&r";
    }

    private String getMarginSpaces(int margin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < margin; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }
}
