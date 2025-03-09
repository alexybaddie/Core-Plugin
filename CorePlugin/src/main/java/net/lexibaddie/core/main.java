package net.lexibaddie.core;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class main extends JavaPlugin {

    // Static instance for easy access in modules.
    private static main instance;

    // Folder where module jars are located.
    private File modulesFolder;
    // Map to store loaded modules; key is the module name (jar file name without .jar).
    private final Map<String, modulecontainer> modules = new HashMap<>();

    // Getter for the instance.
    public static main getInstance() {
        return instance;
    }

    /**
     * Helper method to apply theme colors from the main config.
     * This method translates placeholders (e.g., %primary%) from the "theme" section,
     * applies legacy color codes (&), and converts hex color codes (#RRGGBB) to Minecraft format.
     *
     * @param message The message to translate.
     * @return The formatted message.
     */
    public static String applyColors(String message) {
        if (message == null) return "";
        // Get the theme section from the config.
        ConfigurationSection themeSection = instance.getConfig().getConfigurationSection("theme");
        if (themeSection != null) {
            for (String key : themeSection.getKeys(false)) {
                String placeholder = "%" + key + "%";
                String colorCode = themeSection.getString(key, "");
                message = message.replace(placeholder, colorCode);
            }
        }
        // Translate legacy & color codes.
        message = ChatColor.translateAlternateColorCodes('&', message);
        // Replace hex color codes (e.g., #RRGGBB) with Minecraft's hex format.
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            StringBuilder minecraftHex = new StringBuilder("§x");
            for (char c : hexColor.toCharArray()) {
                minecraftHex.append("§").append(c);
            }
            message = message.replace("#" + hexColor, minecraftHex.toString());
        }
        return message;
    }

    @Override
    public void onEnable() {
        // Set static instance.
        instance = this;

        // Create the config if it doesn't exist.
        saveDefaultConfig();

        // Create the "Modules" folder inside the plugin's data folder.
        modulesFolder = new File(getDataFolder(), "modules");
        if (!modulesFolder.exists()) {
            if (modulesFolder.mkdirs()) {
                getLogger().info("Modules folder created at " + modulesFolder.getAbsolutePath());
            } else {
                getLogger().severe("Failed to create Modules folder!");
                return;
            }
        }

        // Load modules from the folder.
        loadModules();

        // Register the core command (declared in plugin.yml).
        corecommand coreCommand = new corecommand(this);
        getCommand("core").setExecutor(coreCommand);
        getCommand("core").setTabCompleter(coreCommand);
    }

    @Override
    public void onDisable() {
        // Unload all modules.
        for (String moduleName : new ArrayList<>(modules.keySet())) {
            unloadModule(moduleName);
        }
        getLogger().info("Core plugin disabled.");
    }

    // Load all module jars found in the Modules folder.
    public void loadModules() {
        File[] jarFiles = modulesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null) {
            getLogger().warning("No module jars found in: " + modulesFolder.getAbsolutePath());
            return;
        }

        for (File jarFile : jarFiles) {
            loadModuleFromFile(jarFile);
        }
    }

    // Load a module by its name (jar file name without .jar).
    public boolean loadModule(String moduleName) {
        File[] jarFiles = modulesFolder.listFiles((dir, name) -> name.equalsIgnoreCase(moduleName + ".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            getLogger().warning("Module jar " + moduleName + " not found in " + modulesFolder.getAbsolutePath());
            return false;
        }
        return loadModuleFromFile(jarFiles[0]);
    }

    // Internal method to load a module jar file.
    private boolean loadModuleFromFile(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            getLogger().info("Loading module: " + jarFile.getName());
            // Look for module.yml inside the jar.
            JarEntry entry = jar.getJarEntry("module.yml");
            if (entry == null) {
                getLogger().warning("module.yml not found in " + jarFile.getName());
                return false;
            }

            // Load the module.yml configuration.
            InputStream is = jar.getInputStream(entry);
            YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
            String mainClass = moduleConfig.getString("main");
            if (mainClass == null || mainClass.isEmpty()) {
                getLogger().warning("No 'main' class defined in module.yml of " + jarFile.getName());
                return false;
            }

            // Create a URLClassLoader for the jar file.
            URLClassLoader loader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, this.getClassLoader());
            // Load the main class.
            Class<?> clazz = loader.loadClass(mainClass);
            if (!moduleloader.class.isAssignableFrom(clazz)) {
                getLogger().warning("The class " + mainClass + " does not implement the moduleloader interface in " + jarFile.getName());
                return false;
            }

            // Instantiate and initialize the module.
            moduleloader module = (moduleloader) clazz.getDeclaredConstructor().newInstance();
            module.onLoad(this);

            // Use the jar file name (without .jar) as the module's name.
            String moduleName = jarFile.getName().substring(0, jarFile.getName().length() - 4);
            modules.put(moduleName, new modulecontainer(jarFile, module, loader));
            getLogger().info("Module " + mainClass + " loaded successfully from " + jarFile.getName());
            return true;
        } catch (Exception e) {
            getLogger().severe("Error loading module from " + jarFile.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Unload a module by name.
    public boolean unloadModule(String moduleName) {
        modulecontainer container = modules.get(moduleName);
        if (container == null) {
            getLogger().warning("Module " + moduleName + " is not loaded.");
            return false;
        }
        try {
            // If the module implements onUnload, call it.
            container.getModule().onUnload(this);
        } catch (Exception e) {
            getLogger().severe("Error during module onUnload: " + e.getMessage());
            e.printStackTrace();
        }
        try {
            // Attempt to close the URLClassLoader to help unload classes.
            container.getClassLoader().close();
        } catch (Exception e) {
            getLogger().severe("Error closing classloader for module " + moduleName + ": " + e.getMessage());
            e.printStackTrace();
        }
        modules.remove(moduleName);
        getLogger().info("Module " + moduleName + " fully unloaded.");
        return true;
    }

    // Reload a module by name.
    public boolean reloadModule(String moduleName) {
        if (unloadModule(moduleName)) {
            return loadModule(moduleName);
        }
        return false;
    }

    // Reload configs for all loaded modules.
    public void reloadAllModuleConfigs() {
        for (modulecontainer container : modules.values()) {
            try {
                container.getModule().onReload(this);
                getLogger().info("Module " + container.getJarFile().getName() + " config reloaded.");
            } catch (Exception e) {
                getLogger().severe("Error reloading config for module " + container.getJarFile().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Return a list of loaded module names for tab completion.
    public List<String> getModuleNames() {
        return new ArrayList<>(modules.keySet());
    }
}
