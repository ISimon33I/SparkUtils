package com.isimon33i.sparkutils.modules.utilities;

import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

import com.isimon33i.utils.ConfigUtils;

public class PlayerState {
    public long playtime = 0;
    
    public boolean afk = false;
    public long lastAction = 0;
    public boolean playtimeCounterActive = false;
    public long playtimeSegmentStart = 0;
    
    public static final String CONFIG_PATH = "data/%s.yml";
    
    public static void save(JavaPlugin plugin, UUID uuid, PlayerState state) {
        var path = String.format(CONFIG_PATH, uuid);
        var config = ConfigUtils.createConfig(plugin, path, false, false);
        config.set("playtime", state.playtime);
        ConfigUtils.saveConfig(plugin, path, config);
    }
    
    public static PlayerState load(JavaPlugin plugin, UUID uuid) {
        var path = String.format(CONFIG_PATH, uuid);
        var config = ConfigUtils.createConfig(plugin, path, false, false);
        var state = new PlayerState();
        
        state.playtime = config.getLong("playtime");
        
        return state;
    }
}
