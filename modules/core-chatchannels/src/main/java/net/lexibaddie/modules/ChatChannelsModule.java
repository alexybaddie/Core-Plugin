package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.commands.ChatChannelCommand;
import net.lexibaddie.modules.commands.ReflectCommand;
import net.lexibaddie.modules.listeners.ChatChannelListener;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;

public class ChatChannelsModule implements moduleloader {

    private static ChatChannelsModule instance;
    private YamlConfiguration moduleConfig;

    public static ChatChannelsModule getInstance() {
        return instance;
    }

    public YamlConfiguration getModuleConfig() {
        return moduleConfig;
    }

    @Override
    public void onLoad(main plugin) {
        instance = this;

        // 1. Create the "module-configs" folder in the core plugin's data folder.
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        if (!moduleConfigsFolder.exists()) {
            moduleConfigsFolder.mkdirs();
        }

        // 2. Create or load chatchannels.yml.
        File configFile = new File(moduleConfigsFolder, "chatchannels.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/chatchannels.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("chatchannels.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default chatchannels.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create chatchannels.yml: " + e.getMessage());
            }
        }
        moduleConfig = YamlConfiguration.loadConfiguration(configFile);

        // 3. Register the /chat command (with alias /channel) via reflection on the main thread.
        plugin.getServer().getScheduler().runTask(plugin, () -> registerCommands(plugin));

        // 4. Register the chat channel listener.
        plugin.getServer().getPluginManager().registerEvents(new ChatChannelListener(moduleConfig), plugin);
        plugin.getLogger().info("ChatChannelsModule loaded and listener registered.");

        // 5. Initialize the ChatChannelManager with the default channel.
        ChatChannelManager.init(moduleConfig);
    }

    private void registerCommands(main plugin) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Register /chat command with alias /channel.
            ReflectCommand chatCommand = new ReflectCommand(
                    "chat",
                    "Switch, list, or view chat channels",
                    "/chat [channel]",
                    Collections.singletonList("channel"),
                    new ChatChannelCommand(moduleConfig)
            );
            commandMap.register(plugin.getName(), chatCommand);
            plugin.getLogger().info("Registered /chat command with alias /channel via reflection.");
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to register /chat command via reflection: " + e.getMessage());
        }
    }
}
