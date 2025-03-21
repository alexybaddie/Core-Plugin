package net.lexibaddie.modules;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
import net.lexibaddie.modules.events.TablistUpdateEvent;
import net.lexibaddie.modules.listeners.TablistListener;
import net.lexibaddie.modules.listeners.TablistUpdateListener;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class TablistModule implements moduleloader {

    private YamlConfiguration config;

    @Override
    public void onLoad(main plugin) {
        // Create or load tablist.yml from module-configs.
        File moduleConfigsFolder = new File(plugin.getDataFolder(), "module-configs");
        if (!moduleConfigsFolder.exists()) {
            moduleConfigsFolder.mkdirs();
        }
        File configFile = new File(moduleConfigsFolder, "tablist.yml");
        if (!configFile.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/tablist.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    plugin.getLogger().info("tablist.yml created in module-configs folder.");
                } else {
                    plugin.getLogger().warning("Default tablist.yml not found in jar resources.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create tablist.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Register the join listener (so new players get the tablist on join).
        plugin.getServer().getPluginManager().registerEvents(new TablistListener(config, plugin), plugin);
        // Register the custom update listener.
        plugin.getServer().getPluginManager().registerEvents(new TablistUpdateListener(config, plugin), plugin);
        plugin.getLogger().info("TablistModule loaded, listeners registered.");
    }

    @Override
    public void onUnload(main plugin) {
        plugin.getLogger().info("TablistModule unloaded.");
    }

    @Override
    public void onReload(main plugin) {
        // Reload the configuration.
        File moduleConfigsFolder = new File(plugin.getDataFolder(), "module-configs");
        File configFile = new File(moduleConfigsFolder, "tablist.yml");
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("TablistModule configuration reloaded.");
        } else {
            plugin.getLogger().warning("tablist.yml not found during reload.");
        }
        // Fire the custom event to update all players' tablists.
        plugin.getServer().getPluginManager().callEvent(new TablistUpdateEvent());
    }
}
