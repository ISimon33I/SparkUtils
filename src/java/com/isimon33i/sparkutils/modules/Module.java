package com.isimon33i.sparkutils.modules;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import com.isimon33i.utils.lang.LanguageManager;

public abstract class Module implements CommandExecutor, TabCompleter {
    
    protected final JavaPlugin plugin;
    protected final LanguageManager langManager;
    
    public Module(JavaPlugin plugin) {
        this.plugin = plugin;
        langManager = LanguageManager.Instance;
    }
    
    public abstract void onRegister(JavaPlugin plugin);
    public void onUnregister(JavaPlugin plugin){}
    
    protected static void registerCommand(JavaPlugin plugin, String name, Module handler){
        var pluginCommand = plugin.getCommand(name);
        if(pluginCommand != null){
            pluginCommand.setExecutor(handler);
            pluginCommand.setTabCompleter(handler);
        }
    }
}
