package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.commands.ReflectCommand;
import net.lexibaddie.modules.commands.SillyFunCommand;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;

public class SillyFunModule implements moduleloader {

    private static SillyFunModule instance;
    private static main pluginInstance;
    private YamlConfiguration config;

    public static SillyFunModule getInstance() {
        return instance;
    }

    public static main getPlugin() {
        return pluginInstance;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    @Override
    public void onLoad(main plugin) {
        instance = this;
        pluginInstance = plugin;

        // 1. Create the "module-configs" folder if it doesn't exist.
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        if (!moduleConfigsFolder.exists()) {
            moduleConfigsFolder.mkdirs();
        }

        // 2. Create or load sillyfun.yml from module-configs.
        File configFile = new File(moduleConfigsFolder, "sillyfun.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/sillyfun.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("sillyfun.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default sillyfun.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create sillyfun.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 3. Register the /spinaround command (alias /spin) via reflection on the main thread.
        plugin.getServer().getScheduler().runTask(plugin, () -> registerCommands(plugin));
        plugin.getLogger().info("SillyFun module loaded.");
    }

    private void registerCommands(main plugin) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Register /spinaround command with alias /spin.
            ReflectCommand spinCommand = new ReflectCommand(
                    "spinaround",
                    "Spin around another player in a silly fun way",
                    "/spinaround <player|stop>",
                    Collections.singletonList("spin"),
                    new SillyFunCommand(config)
            );
            commandMap.register(plugin.getName(), spinCommand);
            plugin.getLogger().info("Registered /spinaround command with alias /spin via reflection.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register /spinaround command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onUnload(main plugin) {
        // Cleanup any running tasks or listeners if needed.
        plugin.getLogger().info("SillyFun module unloaded.");
    }

    @Override
    public void onReload(main plugin) {
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        File configFile = new File(moduleConfigsFolder, "sillyfun.yml");
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("SillyFun configuration reloaded.");
        } else {
            plugin.getLogger().warning("sillyfun.yml not found during reload.");
        }
    }
}
