package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.commands.MessageCommand;
import net.lexibaddie.modules.commands.ReplyCommand;
import net.lexibaddie.modules.commands.ReflectCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;

public class ChatModule implements moduleloader {

    private static ChatModule instance;
    private YamlConfiguration moduleConfig;

    public static ChatModule getInstance() {
        return instance;
    }

    public YamlConfiguration getModuleConfig() {
        return moduleConfig;
    }

    @Override
    public void onLoad(main plugin) {
        instance = this;

        // 1. Create the "module-configs" folder in the core plugin's data folder
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        if (!moduleConfigsFolder.exists()) {
            moduleConfigsFolder.mkdirs();
        }

        // 2. Create or load chat.yml
        File configFile = new File(moduleConfigsFolder, "chat.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/chat.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("chat.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default chat.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create chat.yml: " + e.getMessage());
            }
        }

        moduleConfig = YamlConfiguration.loadConfiguration(configFile);

        // 3. Schedule command registration on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> registerCommands(plugin));

        // 4. Register event listeners if messaging is enabled in chat.yml
        if (moduleConfig.getBoolean("messaging.enabled")) {
            plugin.getServer().getPluginManager().registerEvents(new EventListener(moduleConfig), plugin);
            plugin.getLogger().info("Join/Quit/Chat events are enabled and registered for messagingmodule.");
        } else {
            plugin.getLogger().info("Messaging events are disabled in chat.yml configuration.");
        }
    }

    private void registerCommands(main plugin) {
        boolean messagingEnabled = moduleConfig.getBoolean("messaging.enabled");
        plugin.getLogger().info("chat.yml check - messaging.enabled: " + messagingEnabled);

        if (!messagingEnabled) {
            plugin.getLogger().info("Messaging commands are disabled in chat.yml configuration.");
            return;
        }

        try {
            // Reflection: get the CommandMap from the server
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Register /message
            ReflectCommand messageCommand = new ReflectCommand(
                    "msg",
                    "Send a private message to a player",
                    "/msg <player> <message>",
            //        Collections.emptyList(),
                    Collections.singletonList("message"),
                    new MessageCommand(moduleConfig)
            );
            commandMap.register(plugin.getName(), messageCommand);
            plugin.getLogger().info("Registered /msg command via reflection.");

            // Register /reply
            ReflectCommand replyCommand = new ReflectCommand(
                    "r",
                    "Reply to the last private message",
                    "/r <message>",
            //        Collections.emptyList(),
                    Collections.singletonList("reply"),
                    new ReplyCommand(moduleConfig)
            );
            commandMap.register(plugin.getName(), messageCommand);
            plugin.getLogger().info("Registered /reply command via reflection.");

        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to register commands via reflection: " + e.getMessage());
        }
    }
}
