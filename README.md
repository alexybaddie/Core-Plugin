# Creating a Custom Module for the Core Plugin

This guide explains how to create a new module for the `net.lexibaddie.core` plugin using the **TemplateModule** as an example.

---

## 1. Overview

A **module** is a self-contained add-on that the core plugin (`net.lexibaddie.core`) can load and unload dynamically. Each module:
- Implements the `net.lexibaddie.core.moduleloader` interface in its main class.
- Includes a `module.yml` file in the JAR with metadata (name, version, main class, and `api-version`).
- Copies or loads its own config file(s) into the `module-configs` folder inside the core plugin’s data directory.
- Optionally registers commands and event listeners using reflection.

The template provided (TemplateModule, TemplateCommand, and TemplateListener) shows how to do all of these tasks.

---

## 2. Project Structure

Your module project should resemble this layout:

```
your-module-project/
├── src/main/java/
│   └── your/name/modules/
│       ├── TemplateModule.java
│       └── commands/
│       │   └── TemplateCommand.java
│       └── listeners/
│       │   └── TemplateListener.java
├── resources/
│   ├── template.yml
│   └── module.yml
└── pom.xml (or gradle build file)
```

> **Important**: Make sure to rename the package from `net.lexibaddie.modules` to `your.username.modules` (or the package structure you prefer).

---

## 3. Copying and Renaming the Template

1. **Copy the Template Files**  
   - Copy the following files into your project:
     - `TemplateModule.java` (main module class)
     - `TemplateCommand.java` (an example command executor)
     - `TemplateListener.java` (an example event listener)
     - `template.yml` (default config file)
     - `module.yml` (module metadata)

2. **Update Package Names**  
   - Open each `.java` file and change the package declaration at the top from:
     ```java
     package net.lexibaddie.modules;
     ```
     to:
     ```java
     package your.name.modules;
     ```
     (Adjust subpackages such as `commands` and `listeners` similarly.)

3. **Adjust Class Names** (Optional)  
   - If you want more descriptive class names, rename `TemplateModule` to something like `MyCustomModule` (and do the same for `TemplateCommand` or `TemplateListener` if desired).  
   - If you rename the classes, remember to also update the `module.yml` file so the `main:` entry points to your new main class.

---

## 4. Understanding the Core Pieces

### TemplateModule.java

- **Implements `moduleloader`**: Your main class must implement the `net.lexibaddie.core.moduleloader` interface.  
- **`onLoad(main plugin)`**:  
  - Creates/loads the `module-configs` folder and copies your config (`template.yml`) from the JAR if it doesn’t exist.  
  - Registers any commands or listeners on the main server thread.  
- **`onUnload(main plugin)`**:  
  - Handles cleanup, such as unregistering commands, listeners, or other resources.  
- **`onReload(main plugin)`**:  
  - Reloads the module configuration to reflect any changes made to your config file.

### TemplateCommand.java

- An example command executor.  
- The command (here, `/template`) retrieves a greeting message from the config and sends it to the player.  
- You can modify or extend command logic as you see fit—just be sure to register the command in your `TemplateModule` class.

### TemplateListener.java

- An example listener that triggers on player join events.  
- Pulls a `join-message` setting from your config.  
- Demonstrates how you can manage event-based logic using your module’s config.

### module.yml

- Declares metadata about your module for the core plugin:
  ```yaml
  name: core-module-template
  version: '1.0'
  main: your.username.modules.TemplateModule
  api-version: '1.20'
  ```
- **`main:`** must point to your module’s main class (the one implementing `moduleloader`).

### template.yml

- A sample configuration file for your module.  
- Stored in your module JAR’s `resources` folder so it can be copied to `module-configs` automatically when the plugin loads.  
- Example:
  ```yaml
  greeting: "&aHello, welcome to the TemplateModule!"
  join-message: "&aWelcome, %player%, to our server!"
  ```

---

## 5. Building and Packaging

1. **Set up your build tool** (Maven, Gradle, etc.).  
2. **Include the core plugin dependency** (`net.lexibaddie.core`).  
3. **Compile** your code into a JAR.  
4. Verify that your `module.yml` and `template.yml` are included in the final JAR root (so they can be accessed at runtime).

---

## 6. Installing the Module

1. **Place the JAR**: Take the newly built module JAR and place it inside the main plugin’s module-loading location (or directly into the server’s `plugins` folder if that’s how your setup is configured).  
2. **Start or Reload the Server**:  
   - The core plugin will detect your module and invoke the `onLoad` method in your module’s main class.  
   - Your config (`template.yml`) should appear in the `module-configs` folder if it wasn’t already there.  
   - Your commands (e.g., `/template`) and listeners (e.g., `PlayerJoinEvent`) should become active.

---

## 7. Customizing Further

- **Add more commands**: Create new classes that implement `CommandExecutor` and register them similarly to `TemplateCommand`.  
- **Add more listeners**: Create classes implementing `Listener` for different events.  
- **Add more config values**: Extend `template.yml` and read them in your code.  
- **Override `onReload` logic**: If your plugin has dynamic changes that need to be applied without a full server restart, handle them here.

---

# Core Module Example

Below are example classes (and their config files) showcasing how to implement a custom module using the `moduleloader` interface and the reflection-based command registration in the **Core Plugin**.

These files collectively demonstrate:
1. How to load a config from resources into `module-configs`.
2. How to register commands via reflection.
3. How to listen to server events.
4. How to set up your `module.yml` to define the module’s main class and metadata.

---

## 1. TemplateModule.java

```java
package your.username.modules;

import your.username.modules.commands.TemplateCommand;
import your.username.modules.commands.ReflectCommand;

import net.lexibaddie.core.main;
import net.lexibaddie.core.moduleloader;
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
```

---

## 2. TemplateCommand.java

```java
package your.username.modules.commands;

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
```

---

## 3. TemplateListener.java

```java
package your.username.modules.listeners;

import net.lexibaddie.core.main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TemplateListener implements Listener {

    private final YamlConfiguration config;

    public TemplateListener(YamlConfiguration config) {
        this.config = config;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Retrieve the join message from the config and send it to the player.
        String joinMessage = config.getString("join-message", "&aWelcome to our server!");
        event.getPlayer().sendMessage(main.applyColors(joinMessage));
    }
}
```

---

## 4. `template.yml`

```yaml
#----------------------------------------------------------#
#           Template Example Configuration File            #
#----------------------------------------------------------#
greeting: "&aHello, welcome to the TemplateModule!"
join-message: "&aWelcome, %player%, to our server!"
```

---

## 5. `module.yml`

```yaml
name: core-module-template
version: '${project.version}'
main: your.username.modules.TemplateModule
api-version: '1.20'
```

---

### How it All Fits Together

1. **Place `module.yml` and `template.yml`** in your module’s resources (root of the JAR) so they can be loaded at runtime.
2. **Compile your module** into a JAR with the above classes.
3. **Drop the JAR** into the appropriate directory where the `net.lexibaddie.core` plugin loads modules.
4. **Start or reload** the server; the `TemplateModule`’s `onLoad` method will run, copying `template.yml` into the `module-configs` folder (if it doesn’t already exist), registering a command, and setting up a listener.

By following this template, you can create your own fully customized modules—just rename classes, packages, and configuration files to suit your project’s needs.


### Questions or Issues?

If you run into any problems or have suggestions for improvements, feel free to open an issue or submit a pull request.
