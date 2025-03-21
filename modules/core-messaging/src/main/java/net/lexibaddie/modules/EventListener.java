package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventListener implements Listener {

    private final YamlConfiguration config;

    public EventListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!config.getBoolean("messaging.enabled", false)) {
            return;
        }

        // Get the chat format with applied colors.
        String chatFormat = main.applyColors(
                config.getString("messaging.chat.chat_format", "%secondary%%player% %primary%» %secondary%%message%")
        );

        // Determine the reset color by inspecting the part before "%message%" using regex.
        String resetColor = ChatColor.RESET.toString();
        int messageIndex = chatFormat.indexOf("%message%");
        if (messageIndex != -1) {
            String formatPrefix = chatFormat.substring(0, messageIndex);
            // This pattern matches either a hex color code (e.g., "§x§f§f§a§4§b§f")
            // or a legacy color code (e.g., "§a").
            Pattern colorPattern = Pattern.compile("(§x(§[0-9A-Fa-f]){6}|§[0-9A-Fa-f])");
            Matcher matcher = colorPattern.matcher(formatPrefix);
            String lastMatch = "";
            while (matcher.find()) {
                lastMatch = matcher.group();
            }
            if (!lastMatch.isEmpty()) {
                resetColor = lastMatch;
            }
        }

        String senderDisplayName = event.getPlayer().getDisplayName();
        // Process the raw message into one with actual § color codes.
        String coloredMessage = main.applyColors(event.getMessage());

        // Check if any online player's name is mentioned.
        boolean nameFound = false;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (coloredMessage.contains(online.getName())) {
                nameFound = true;
                break;
            }
        }

        // If no name is mentioned, use the default format.
        if (!nameFound) {
            event.setFormat(chatFormat
                    .replace("%player%", senderDisplayName)
                    .replace("%message%", coloredMessage)
            );
            return;
        }

        // Cancel the event to send custom messages per recipient.
        event.setCancelled(true);
        for (Player recipient : event.getRecipients()) {
            String finalMessage = coloredMessage;
            if (coloredMessage.contains(recipient.getName())) {
                // Replace the player's name with a highlighted version.
                // The name is now green and then reset to the extracted resetColor.
                String highlightedName = ChatColor.GREEN + recipient.getName() + resetColor;
                finalMessage = coloredMessage.replace(recipient.getName(), highlightedName);

                // Play the ping sound (ENTITY_EXPERIENCE_ORB_PICKUP) for this recipient.
                Bukkit.getScheduler().runTask(main.getInstance(), () -> {
                    recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                });
            }
            // Build and send the formatted message.
            String formattedMessage = chatFormat
                    .replace("%player%", senderDisplayName)
                    .replace("%message%", finalMessage);
            recipient.sendMessage(formattedMessage);
        }
    }
}
