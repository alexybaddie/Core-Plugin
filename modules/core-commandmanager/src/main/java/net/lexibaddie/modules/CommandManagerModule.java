package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.commands.ReflectCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CommandManagerModule implements moduleloader {

    private static CommandManagerModule instance;
    private YamlConfiguration moduleConfig;

    public static CommandManagerModule getInstance() {
        return instance;
    }

    public YamlConfiguration getModuleConfig() {
        return moduleConfig;
    }

    @Override
    public void onLoad(main plugin) {
        instance = this;

        // 1. Create or load the "module-configs" folder.
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        if (!moduleConfigsFolder.exists()) {
            moduleConfigsFolder.mkdirs();
        }

        // 2. Create or load commandmanager.yml.
        File configFile = new File(moduleConfigsFolder, "commandmanager.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/commandmanager.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("commandmanager.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default commandmanager.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create commandmanager.yml: " + e.getMessage());
            }
        }
        moduleConfig = YamlConfiguration.loadConfiguration(configFile);

        // 3. Wait for a delay (e.g., 100 ticks) before processing command mappings.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> processCommandMappings(plugin), 100L);
        plugin.getLogger().info("CommandManagerModule loaded and scheduled command mappings processing.");
    }

    private void processCommandMappings(main plugin) {
        try {
            // Access the CommandMap via reflection.
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Get the internal "knownCommands" field.
            Field knownCommandsField = getField(commandMap.getClass(), "knownCommands");

            // Get the commands configuration section.
            ConfigurationSection commandsSection = moduleConfig.getConfigurationSection("commands");
            if (commandsSection == null) {
                plugin.getLogger().warning("No 'commands' section found in commandmanager.yml.");
                return;
            }

            for (String originalKey : commandsSection.getKeys(false)) {
                // The key is the original command name (without the leading slash)
                String original = originalKey;

                boolean disable = commandsSection.getBoolean(original + ".disable", false);
                String replace = commandsSection.getString(original + ".replace", "").trim();
                List<String> aliases = commandsSection.getStringList(original + ".aliases");

                // Try to locate the command by scanning the known commands map.
                Command origCommand = findCommand(commandMap, knownCommandsField, original);
                if (origCommand == null) {
                    plugin.getLogger().warning("Command '" + original + "' not found in CommandMap. Skipping.");
                    continue;
                }

                if (disable) {
                    // Disable the command.
                    if (origCommand instanceof PluginCommand) {
                        // For plugin.yml registered commands, override their executor.
                        ((PluginCommand) origCommand).setExecutor(new DisabledCommandExecutor());
                        plugin.getLogger().info("Command '" + original + "' has been disabled (executor overridden).");
                    } else {
                        // For reflection-based commands, remove them from the map.
                        removeCommandFromMap(commandMap, knownCommandsField, origCommand);
                        plugin.getLogger().info("Command '" + original + "' has been disabled (removed from map).");
                    }
                } else if (!replace.isEmpty() || (aliases != null && !aliases.isEmpty())) {
                    // Remap the command.
                    if (origCommand instanceof PluginCommand) {
                        // For plugin.yml commands, override the executor.
                        ((PluginCommand) origCommand).setExecutor(new RemappedCommandExecutor(origCommand));
                        plugin.getLogger().info("Command '" + original + "' has been remapped (executor overridden).");
                    } else {
                        // For reflection commands, remove and re-register.
                        removeCommandFromMap(commandMap, knownCommandsField, origCommand);
                        String finalName = !replace.isEmpty() ? replace : original;
                        List<String> finalAliases = (aliases != null && !aliases.isEmpty()) ? aliases : origCommand.getAliases();

                        ReflectCommand remappedCommand = new ReflectCommand(
                                finalName,
                                "Remapped command from " + original,
                                (origCommand.getUsage() != null && !origCommand.getUsage().isEmpty()) ? origCommand.getUsage() : "/" + finalName,
                                finalAliases,
                                new RemappedCommandExecutor(origCommand)
                        );
                        commandMap.register(plugin.getName(), remappedCommand);
                        plugin.getLogger().info("Command '" + original + "' has been remapped to '" + finalName + "' with aliases " + finalAliases + ".");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to process command mappings: " + e.getMessage());
        }
    }

    // Helper method to find a command by name (ignoring case) in the knownCommands map.
    private Command findCommand(CommandMap commandMap, Field knownCommandsField, String name) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            for (Command cmd : knownCommands.values()) {
                if (cmd.getName().equalsIgnoreCase(name)) {
                    return cmd;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Updated helper method to remove a command from the knownCommands map.
    private void removeCommandFromMap(CommandMap commandMap, Field knownCommandsField, Command command) {
        try {
            // If possible, invoke the command's unregister method.
            try {
                Method unregisterMethod = command.getClass().getDeclaredMethod("unregister", CommandMap.class);
                unregisterMethod.setAccessible(true);
                unregisterMethod.invoke(command, commandMap);
            } catch (Exception ex) {
                // Not all commands have an unregister method; ignore.
            }

            @SuppressWarnings("unchecked")
            Map<String, Command> knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
            // Create a modifiable copy.
            Map<String, Command> modifiable = new HashMap<>(knownCommands);
            // Remove entries that reference the command.
            Iterator<Map.Entry<String, Command>> iterator = modifiable.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Command> entry = iterator.next();
                if (entry.getValue().equals(command)) {
                    iterator.remove();
                }
            }
            // Set the updated map back.
            knownCommandsField.set(commandMap, modifiable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to locate a field in the class hierarchy.
    private Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy.");
    }

    @Override
    public void onUnload(main plugin) {
        plugin.getLogger().info("CommandManagerModule unloaded.");
    }

    @Override
    public void onReload(main plugin) {
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        File configFile = new File(moduleConfigsFolder, "commandmanager.yml");
        if (configFile.exists()) {
            moduleConfig = YamlConfiguration.loadConfiguration(configFile);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> processCommandMappings(plugin), 100L);
            plugin.getLogger().info("CommandManagerModule configuration reloaded.");
        } else {
            plugin.getLogger().warning("commandmanager.yml not found during reload.");
        }
    }

    // A CommandExecutor that disables the command by doing nothing.
    private static class DisabledCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
            sender.sendMessage(net.lexibaddie.core.main.applyColors("&cThis command is disabled."));
            return true;
        }
    }

    // A CommandExecutor that delegates execution to the original command.
    private static class RemappedCommandExecutor implements CommandExecutor {
        private final Command originalCommand;

        public RemappedCommandExecutor(Command originalCommand) {
            this.originalCommand = originalCommand;
        }

        @Override
        public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
            return originalCommand.execute(sender, label, args);
        }
    }
}
