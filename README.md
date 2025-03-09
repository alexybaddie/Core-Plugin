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

> **Important**: Make sure to rename the package from `net.lexibaddie.modules` to `your.name.modules` (or the package structure you prefer).

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
  main: your.name.modules.TemplateModule
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
2. **Include the core plugin dependency** (`net.lexibaddie.core`) if needed.  
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

### Questions or Issues?

If you run into any problems or have suggestions for improvements, feel free to open an issue or submit a pull request.
