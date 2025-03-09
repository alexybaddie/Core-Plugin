package net.lexibaddie.modules.commands;

import net.lexibaddie.core.main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {

    private final YamlConfiguration config;

    public ReplyCommand(YamlConfiguration config) {
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

        if (args.length < 1) {
            sender.sendMessage(main.applyColors(
                    config.getString("commands.messaging.reply.usage", "/r <message>")
            ));
            return true;
        }

        Player player = (Player) sender;
        Player target = MessageCommand.getLastMessaged().get(player);

        if (target == null || !target.isOnline()) {
            player.sendMessage(main.applyColors(config.getString("general.player_not_found")));
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }

        String formattedMessage = main.applyColors(
                config.getString("messaging.direct.to_format")
                        .replace("%target%", target.getName())
                        .replace("%message%", message.toString().trim())
        );
        player.sendMessage(formattedMessage);

        String targetMessage = main.applyColors(
                config.getString("messaging.direct.from_format")
                        .replace("%sender%", player.getName())
                        .replace("%message%", message.toString().trim())
        );
        target.sendMessage(targetMessage);

        // Update the last messaged map to allow continued replies
        MessageCommand.getLastMessaged().put(player, target);
        MessageCommand.getLastMessaged().put(target, player);

        return true;
    }
}
