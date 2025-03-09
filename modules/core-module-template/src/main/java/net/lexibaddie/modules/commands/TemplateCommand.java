package net.lexibaddie.modules.commands;

import net.lexibaddie.core.main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TemplateCommand implements CommandExecutor {

    private final YamlConfiguration config;

    public TemplateCommand(YamlConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(main.applyColors("&cOnly players can use this command."));
            return true;
        }
        Player player = (Player) sender;

        // Retrieve the greeting from config and send it to the player.
        String greeting = config.getString("greeting", "&aHello, world!");
        player.sendMessage(main.applyColors(greeting));

        return true;
    }
}
