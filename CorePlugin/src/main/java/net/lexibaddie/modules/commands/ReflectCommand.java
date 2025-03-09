package net.lexibaddie.modules.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

import java.util.List;

/**
 * A simple wrapper that delegates command execution to a CommandExecutor.
 */
public class ReflectCommand extends BukkitCommand {

    private final org.bukkit.command.CommandExecutor executor;

    public ReflectCommand(String name, String description, String usageMessage, List<String> aliases, org.bukkit.command.CommandExecutor executor) {
        super(name);
        this.description = description;
        this.usageMessage = usageMessage;
        if (aliases != null) {
            setAliases(aliases);
        }
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return executor.onCommand(sender, this, commandLabel, args);
    }
}
