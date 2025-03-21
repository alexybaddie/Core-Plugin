package net.lexibaddie.modules.commands;

import net.lexibaddie.core.main;
import net.lexibaddie.modules.SillyFunModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SillyFunCommand implements CommandExecutor {

    private final YamlConfiguration config;
    // Map to keep track of spinning tasks for each player.
    private static final Map<UUID, Integer> spinningTasks = new HashMap<>();

    public SillyFunCommand(YamlConfiguration config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Only players may execute this command.
        if (!(sender instanceof Player)) {
            sender.sendMessage(main.applyColors("&cOnly players can use this command."));
            return true;
        }
        Player spinner = (Player) sender;

        if (args.length < 1) {
            spinner.sendMessage(main.applyColors("&cUsage: /spinaround <player|stop>"));
            return true;
        }

        // Check if the command is to stop spinning.
        if (args[0].equalsIgnoreCase("stop")) {
            if (spinningTasks.containsKey(spinner.getUniqueId())) {
                Bukkit.getScheduler().cancelTask(spinningTasks.get(spinner.getUniqueId()));
                spinningTasks.remove(spinner.getUniqueId());
                spinner.sendMessage(main.applyColors("&aSpinning stopped."));
                spinner.setFlying(false);
                spinner.setAllowFlight(false);
            } else {
                spinner.sendMessage(main.applyColors("&cYou are not currently spinning around anyone."));
            }
            return true;
        }

        // Otherwise, try to get the target player.
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            spinner.sendMessage(main.applyColors("&cTarget player not found or offline."));
            return true;
        }

        // Cancel any existing spinning task for this player.
        if (spinningTasks.containsKey(spinner.getUniqueId())) {
            Bukkit.getScheduler().cancelTask(spinningTasks.get(spinner.getUniqueId()));
            spinningTasks.remove(spinner.getUniqueId());
        }

        // Get configuration parameters.
        double height = config.getDouble("height", 1.0);
        double distance = config.getDouble("distance", 3.0);
        double speed = config.getDouble("speed", 0.32); // Radians to increment per tick.

        // Enable flight for smoother movement.
        spinner.setAllowFlight(true);
        spinner.setFlying(true);

        // Start a repeating task to spin the player around the target.
        BukkitTask task = new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                // If either player goes offline, cancel the task.
                if (!spinner.isOnline() || !target.isOnline()) {
                    spinner.sendMessage(main.applyColors("&cSpinning cancelled as one of the players went offline."));
                    spinningTasks.remove(spinner.getUniqueId());
                    cancel();
                    return;
                }
                // Get the target's current location.
                Location targetLoc = target.getLocation();
                // Compute the new location for the spinner.
                double offsetX = distance * Math.cos(angle);
                double offsetZ = distance * Math.sin(angle);
                Location newLoc = targetLoc.clone().add(offsetX, height, offsetZ);
                // Set the spinner's direction to look at the target.
                newLoc.setDirection(targetLoc.clone().subtract(newLoc).toVector());
                spinner.teleport(newLoc);
                angle += speed;
            }
        }.runTaskTimer(SillyFunModule.getPlugin(), 0L, 1L);

        // Store the task ID for future cancellation.
        spinningTasks.put(spinner.getUniqueId(), task.getTaskId());
        spinner.sendMessage(main.applyColors("&aStarted spinning around " + target.getName() + ". Use /spinaround stop to cancel."));
        return true;
    }
}
