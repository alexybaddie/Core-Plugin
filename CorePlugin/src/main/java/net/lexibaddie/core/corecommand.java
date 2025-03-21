package net.lexibaddie.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class corecommand implements CommandExecutor, TabCompleter {

    private final main plugin;

    public corecommand(main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(main.applyColors("&cUsage: /core <reload/module>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            // Reload all modules.
        //    plugin.loadModules();
            plugin.reloadAllModuleConfigs();
            sender.sendMessage(main.applyColors("&aCore & Modules reloaded."));
            return true;
        } else if (args[0].equalsIgnoreCase("module")) {
            if (args.length < 2) {
                sender.sendMessage(main.applyColors("&cUsage: /core module <load/unload/reload/list> [module]"));
                return true;
            }
            String action = args[1];
            if (action.equalsIgnoreCase("load")) {
                if (args.length < 3) {
                    sender.sendMessage(main.applyColors("&cUsage: /core module load <module>"));
                    return true;
                }
                String moduleName = args[2];
                if (plugin.loadModule(moduleName)) {
                    sender.sendMessage(main.applyColors("&aModule " + moduleName + " loaded."));
                } else {
                    sender.sendMessage(main.applyColors("&cFailed to load module " + moduleName + "."));
                }
                return true;
            } else if (action.equalsIgnoreCase("unload")) {
                if (args.length < 3) {
                    sender.sendMessage(main.applyColors("&cUsage: /core module unload <module>"));
                    return true;
                }
                String moduleName = args[2];
                if (plugin.unloadModule(moduleName)) {
                    sender.sendMessage(main.applyColors("&aModule " + moduleName + " unloaded."));
                } else {
                    sender.sendMessage(main.applyColors("&cFailed to unload module " + moduleName + "."));
                }
                return true;
            } else if (action.equalsIgnoreCase("reload")) {
                if (args.length < 3) {
                    sender.sendMessage(main.applyColors("&cUsage: /core module reload <module>"));
                    return true;
                }
                String moduleName = args[2];
                if (plugin.reloadModule(moduleName)) {
                    sender.sendMessage(main.applyColors("&aModule " + moduleName + " reloaded."));
                } else {
                    sender.sendMessage(main.applyColors("&cFailed to reload module " + moduleName + "."));
                }
                return true;
            } else if (action.equalsIgnoreCase("list")) {
                // List loaded and unloaded modules.
                // 1. Get all module jar names from the Modules folder.
                File modulesFolder = new File(plugin.getDataFolder(), "Modules");
                File[] jarFiles = modulesFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                List<String> allModules = new ArrayList<>();
                if (jarFiles != null) {
                    for (File jar : jarFiles) {
                        String name = jar.getName();
                        if (name.toLowerCase().endsWith(".jar")) {
                            name = name.substring(0, name.length() - 4);
                        }
                        allModules.add(name);
                    }
                }
                // 2. Get loaded modules from the core plugin.
                List<String> loadedModules = plugin.getModuleNames();
                // 3. Compute unloaded modules.
                List<String> unloadedModules = new ArrayList<>();
                for (String mod : allModules) {
                    if (!loadedModules.contains(mod)) {
                        unloadedModules.add(mod);
                    }
                }
                // 4. Build message.
                StringBuilder sb = new StringBuilder();
                sb.append("\n&f                           &bLoaded Modules");
                sb.append("\n&7&m                                                                           &r\n \n&a");
                if(!loadedModules.isEmpty()) {
                    sb.append(loadedModules.isEmpty() ? "None" : String.join("&f, &a", loadedModules));
                } else if(!unloadedModules.isEmpty()) {
                    if(!loadedModules.isEmpty()) {
                        sb.append("&f, &c\n");
                    }
                    sb.append("&c");
                    sb.append(unloadedModules.isEmpty() ? "None" : String.join("&f, &c", unloadedModules));
                }
                sb.append("\n \n&7&m                                                                           &r\n ");
                sender.sendMessage(main.applyColors(sb.toString()));
                return true;
            }
        }
        sender.sendMessage(main.applyColors("&cInvalid command usage."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            if ("module".startsWith(args[0].toLowerCase())) {
                completions.add("module");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("module")) {
            for (String sub : new String[]{"load", "unload", "reload", "list"}) {
                if (sub.startsWith(args[1].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("module") &&
                (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase("unload") || args[1].equalsIgnoreCase("reload"))) {
            String partial = args[2].toLowerCase();
            for (String mod : plugin.getModuleNames()) {
                if (mod.toLowerCase().startsWith(partial)) {
                    completions.add(mod);
                }
            }
        }
        return completions;
    }
}
