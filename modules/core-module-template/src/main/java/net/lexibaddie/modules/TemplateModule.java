package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.commands.TemplateCommand;
import net.lexibaddie.modules.commands.ReflectCommand;
import net.lexibaddie.modules.listeners.TemplateListener;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Collections;

public class TemplateModule implements moduleloader {

    private static TemplateModule instance;
    private YamlConfiguration moduleConfig;

    public static TemplateModule getInstance() {
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

        // 2. Create or load template.yml.
        File configFile = new File(moduleConfigsFolder, "template.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/template.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("template.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default template.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create template.yml: " + e.getMessage());
            }
        }
        moduleConfig = YamlConfiguration.loadConfiguration(configFile);

        // 3. Register the /template command (with alias /temp) via reflection on the main thread.
        plugin.getServer().getScheduler().runTask(plugin, () -> registerCommands(plugin));

        // 4. Register an example listener.
        plugin.getServer().getPluginManager().registerEvents(new TemplateListener(moduleConfig), plugin);
        plugin.getLogger().info("TemplateModule loaded and listener registered.");
    }

    private void registerCommands(main plugin) {
        try {
            Field commandMapField = plugin.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(plugin.getServer());

            // Register /template command with alias /temp.
            ReflectCommand templateCommand = new ReflectCommand(
                    "template",
                    "A template command for demonstration",
                    "/template [args]",
                    Collections.singletonList("temp"),
                    new TemplateCommand(moduleConfig)
            );
            commandMap.register(plugin.getName(), templateCommand);
            plugin.getLogger().info("Registered /template command with alias /temp via reflection.");
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLogger().severe("Failed to register /template command via reflection: " + e.getMessage());
        }
    }

    @Override
    public void onUnload(main plugin) {
        // Add any cleanup code here if needed (for example, unregister listeners or commands).
        plugin.getLogger().info("TemplateModule unloaded.");
    }

    @Override
    public void onReload(main plugin) {
        // Reload configuration from template.yml.
        File coreFolder = plugin.getDataFolder();
        File moduleConfigsFolder = new File(coreFolder, "module-configs");
        File configFile = new File(moduleConfigsFolder, "template.yml");
        if (configFile.exists()) {
            moduleConfig = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("TemplateModule configuration reloaded.");
        } else {
            plugin.getLogger().warning("template.yml not found during reload.");
        }
    }
}
