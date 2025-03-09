package net.lexibaddie.modules.commands;

import net.lexibaddie.core.main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageCommand implements CommandExecutor {

    private static final Map<Player, Player> lastMessaged = new HashMap<>();
    private final YamlConfiguration config;

    public MessageCommand(YamlConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(main.applyColors(config.getString("general.only_players")));
            return true;
        }

        if (!config.getBoolean("messaging.enabled")) {
            sender.sendMessage(main.applyColors(config.getString("general.command_disabled")));
            return true;
        }

        if (!sender.hasPermission(config.getString("messaging.permission"))) {
            sender.sendMessage(main.applyColors(config.getString("general.no_permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(main.applyColors(
                    config.getString("commands.messaging.msg.usage", "/msg <player> <message>")
            ));
            return true;
        }

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(main.applyColors(config.getString("general.player_not_found")));
            return true;
        }

        // Build the message from remaining args
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(args[i]).append(" ");
        }

        // Send the message to the sender
        String formattedMessage = main.applyColors(
                config.getString("messaging.direct.to_format")
                        .replace("%target%", target.getName())
                        .replace("%message%", message.toString().trim())
        );
        player.sendMessage(formattedMessage);

        // Send the message to the target
        String targetMessage = main.applyColors(
                config.getString("messaging.direct.from_format")
                        .replace("%sender%", player.getName())
                        .replace("%message%", message.toString().trim())
        );
        target.sendMessage(targetMessage);

        lastMessaged.put(player, target);
        lastMessaged.put(target, player);

        return true;
    }

    public static Map<Player, Player> getLastMessaged() {
        return lastMessaged;
    }
}
