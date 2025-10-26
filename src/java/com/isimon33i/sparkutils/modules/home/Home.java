package com.isimon33i.sparkutils.modules.home;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

public class Home {

    public String displayName;
    public String description;
    public Location location;
    public Material icon;

    public boolean teleportEntity(Entity entity) {
        if (isValid(entity)) {
            entity.teleport(location);
            return true;
        }
        return false;
    }

    public boolean isValid(CommandSender sender) {
        if (location.getWorld() == null) {
            sender.sendMessage("Invalid home: No World");
            return false;
        }
        return true;
    }

    public void serialize(ConfigurationSection warp) {
        var world = location.getWorld();
        warp.set("display_name", displayName);
        warp.set("description", description);
        if(icon != null){
            var nkey = icon.getKeyOrNull();
            if(nkey != null) {
                warp.set("icon", nkey.toString());
            }
        }
        warp.set("world", world != null ? world.getName() : "");
        warp.set("x", location.getX());
        warp.set("y", location.getY());
        warp.set("z", location.getZ());
        warp.set("yaw", location.getYaw());
        warp.set("pitch", location.getPitch());
    }

    public void deserialize(ConfigurationSection warp) {
        displayName = warp.getString("display_name");
        description = warp.getString("description");
        var iconMaterialName = warp.getString("icon");
        icon = iconMaterialName != null ? Material.matchMaterial(iconMaterialName) : Material.GRASS_BLOCK;
        var worldName = warp.getString("world");
        location = new Location(
                worldName != null ? Bukkit.getWorld(worldName) : null,
                warp.getDouble("x"),
                warp.getDouble("y"),
                warp.getDouble("z"),
                (float) warp.getDouble("yaw"),
                (float) warp.getDouble("pitch")
        );
    }
}
