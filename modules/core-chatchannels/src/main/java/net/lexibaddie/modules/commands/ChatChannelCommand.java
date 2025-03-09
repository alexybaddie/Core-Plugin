package net.lexibaddie.modules.commands;

import net.lexibaddie.core.main;
import net.lexibaddie.modules.ChatChannelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;

public class ChatChannelCommand implements CommandExecutor {

    private final YamlConfiguration config;

    public ChatChannelCommand(YamlConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(main.applyColors("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        // If no arguments, list available channels and show current channel.
        if (args.length == 0) {
            String current = ChatChannelManager.getChannel(player);
            Set<String> channels = config.getConfigurationSection("channels").getKeys(false);
            StringBuilder sb = new StringBuilder();
            sb.append(main.applyColors("&aCurrent channel: ")).append(current).append("\n");
            sb.append(main.applyColors("&aAvailable channels: ")).append(String.join(", ", channels));
            player.sendMessage(sb.toString());
            return true;
        }

        String input = args[0].toLowerCase();
        String targetChannel = null;
        // Loop through channels and check channel names and aliases.
        for (String channel : config.getConfigurationSection("channels").getKeys(false)) {
        //    main.getInstance().getLogger().info("Checking channel '" + channel + "' against input '" + input + "'");
            if (channel.equalsIgnoreCase(input)) {
                targetChannel = channel;
                break;
            }
            for (String alias : config.getStringList("channels." + channel + ".aliases")) {
        //        main.getInstance().getLogger().info("Checking alias '" + alias + "' for channel '" + channel + "'");
                if (alias.equalsIgnoreCase(input)) {
                    targetChannel = channel;
                    break;
                }
            }
            if (targetChannel != null) break;
        }
    //    main.getInstance().getLogger().info("Target channel determined as: " + targetChannel);
        if (targetChannel == null) {
            player.sendMessage(main.applyColors("&cChannel '" + input + "' does not exist."));
            return true;
        }
        // For non-default channels, check permission.
        if (!targetChannel.equalsIgnoreCase("all")) {
            String perm = config.getString("channels." + targetChannel + ".permission", "");
            if (!perm.isEmpty() && !player.hasPermission(perm)) {
                player.sendMessage(main.applyColors("&cYou don't have permission to join channel '" + targetChannel.toUpperCase()));
                return true;
            }
        }
        ChatChannelManager.setChannel(player, targetChannel);
    //    main.getInstance().getLogger().info("Set channel for " + player.getName() + " to " + targetChannel);
        String display = config.getString("channels." + targetChannel + ".display", targetChannel);
        player.sendMessage(main.applyColors("&aYou have switched to channel " + display.toUpperCase()));
        return true;
    }
}
