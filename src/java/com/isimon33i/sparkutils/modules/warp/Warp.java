package com.isimon33i.sparkutils.modules.warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.isimon33i.sparkutils.Main;

public class Warp {

    public String displayName;
    public String description;
    public Location location;
    public boolean restricted;
    public Material icon;
    @Nullable public String serverName;

    public boolean teleportEntity(Entity entity) {
        
        if(serverName != null) {
            if (entity instanceof Player player) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(serverName);
                var plugin = Main.GetInstance();
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                return true;
            }
        } else {
            if (isValid()) {
                entity.teleport(location);
                return true;
            }else{
                entity.sendMessage("Invalid warp: No World");
            }
        }
        return false;
    }

    public boolean isValid() {
        return location.getWorld() != null;
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
        if(serverName != null) warp.set("server", serverName);
        warp.set("world", world != null ? world.getName() : "");
        warp.set("x", location.getX());
        warp.set("y", location.getY());
        warp.set("z", location.getZ());
        warp.set("yaw", location.getYaw());
        warp.set("pitch", location.getPitch());
        warp.set("restricted", restricted);
    }

    public void deserialize(ConfigurationSection warp) {
        displayName = warp.getString("display_name");
        description = warp.getString("description");
        var iconMaterialName = warp.getString("icon");
        icon = iconMaterialName != null ? Material.matchMaterial(iconMaterialName) : Material.GRASS_BLOCK;
        serverName = warp.getString("server");
        var worldName = warp.getString("world");
        location = new Location(
                worldName != null ? Bukkit.getWorld(worldName) : null,
                warp.getDouble("x"),
                warp.getDouble("y"),
                warp.getDouble("z"),
                (float) warp.getDouble("yaw"),
                (float) warp.getDouble("pitch")
        );
        restricted = warp.getBoolean("restricted");
    }
}
