package com.isimon33i.sparkutils.modules;

import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.lang.Placeholder;

public class Spawn extends Module {

    FileConfiguration spawnConfig;
    final String spawnConfigFilePath = "spawn.yml";

    public Spawn(Main plugin) {
        super(plugin);
    }

    @Override
    public void onRegister() {
        registerCommand(plugin, "spawn", this);
        registerCommand(plugin, "spawnall", this);

        spawnConfig = ConfigUtils.createConfig(plugin, spawnConfigFilePath, true, false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        if(sender == null) return false;
        final Player player;
        final String srcDisplayName;
        final String locale;
        if (sender instanceof Player p) {
            player = p;
            locale = player.getLocale();
            srcDisplayName = player.getDisplayName();
        }else{
            player = null;
            locale = "en_US";
            srcDisplayName = langManager.getMessage("console_display_name", locale);
        }
        var spawnWorld = getSpawnWorld();
        if(spawnWorld == null){
            sender.sendMessage(langManager.getMessage("spawn.no_spawn_world", locale));
            return true;
        }
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (args.length == 0) {
                if (player != null) {
                    if (sender.hasPermission("sparkutils.spawn.self")) {
                        player.sendMessage(langManager.getMessage("spawn.teleporting", locale, new Placeholder("srcPlayer", srcDisplayName)));
                        player.teleport(spawnWorld.getSpawnLocation());
                    } else {
                        player.sendMessage(langManager.getMessage("spawn.permission_denied.self", locale, new Placeholder("srcPlayer", srcDisplayName)));
                    }
                } else {
                    sender.sendMessage(langManager.getMessage("spawn.console_cant_tp", locale));
                }
            } else if (args.length == 1) {
                if (sender.hasPermission("sparkutils.spawn.other")) {
                    var targetPlayer = plugin.getServer().getPlayer(args[0]);
                    if (targetPlayer != null) {
                        sender.sendMessage(langManager.getMessage("spawn.teleporting.other", locale, new Placeholder("srcPlayer", srcDisplayName), new Placeholder("targetPlayer", targetPlayer.getDisplayName())));
                        targetPlayer.teleport(spawnWorld.getSpawnLocation());
                        targetPlayer.sendMessage(langManager.getMessage("spawn.teleported.self", locale, new Placeholder("srcPlayer", targetPlayer.getDisplayName())));
                    } else {
                        sender.sendMessage(langManager.getMessage("spawn.teleporting.other.not_found", locale, new Placeholder("srcPlayer", srcDisplayName), new Placeholder("targetPlayer", args[0])));
                    }
                } else {
                    sender.sendMessage(langManager.getMessage("spawn.permission_denied.other", locale, new Placeholder("srcPlayer", srcDisplayName)));
                }

            }
            return true;
        } else if (command.getName().equalsIgnoreCase("spawnall")) {
            var srcPlayerPlaceholder = new Placeholder("srcPlayer", srcDisplayName);
            if (sender.hasPermission("sparkutils.spawn.all")) {
                sender.sendMessage(langManager.getMessage("spawn.teleporting.all", locale, srcPlayerPlaceholder));
                var worldName = spawnConfig.getString("world");
                if (worldName == null) {
                    sender.sendMessage("World missing from config!");
                    return true;
                }
                var world = plugin.getServer().getWorld(worldName);
                if (world == null) {
                    sender.sendMessage("World not found!");
                    return true;
                }

                plugin.getServer().getOnlinePlayers().forEach(x -> {
                    x.teleport(world.getSpawnLocation());
                    x.sendMessage(langManager.getMessage("spawn.teleported.self", locale, srcPlayerPlaceholder, new Placeholder("targetPlayer", x.getDisplayName())));
                });
            } else {
                sender.sendMessage(langManager.getMessage("spawn.permission_denied.all", locale, srcPlayerPlaceholder));
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String string, String[] args) {
        return null;
    }
    
    public World getSpawnWorld(){
        String worldName = Objects.requireNonNullElse(spawnConfig.getString("world"), "world");
        return Bukkit.getWorld(worldName);
    }
}
