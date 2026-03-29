package com.isimon33i.utils;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigUtils {

    private ConfigUtils() {
    }

    public static FileConfiguration createConfig(JavaPlugin plugin, String path, boolean saveDefaultResource, boolean replace) {
        var configFile = new File(plugin.getDataFolder(), path);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            if (saveDefaultResource) {
                plugin.saveResource(path, replace); 
            }
        }
        
        var config = YamlConfiguration.loadConfiguration(configFile);
        
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return config;
    }

    @SuppressWarnings("CallToPrintStackTrace")
    public static void saveConfig(JavaPlugin plugin, String path, FileConfiguration config) {
        var configFile = new File(plugin.getDataFolder(), path);
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
