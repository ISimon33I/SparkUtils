package com.isimon33i.sparkutils.modules;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ChatUtils;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.lang.Placeholder;

import net.milkbowl.vault.chat.Chat;

public class ChatFormater extends Module implements Listener {
    
    private static Chat chat = null;
    
    final String chatConfigFilePath = "chat.yml";
    FileConfiguration chatConfig;
    
    public ChatFormater(Main plugin) {
        super(plugin);
    }

    @Override
    public void onRegister() {
        var rsp = plugin.getServer().getServicesManager().getRegistration(Chat.class);
        chat = rsp.getProvider();
        if(chat == null) return;
        
        chatConfig = ConfigUtils.createConfig(plugin, chatConfigFilePath, true, false);
        if(!chatConfig.getBoolean("enabled")) return;
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            refreshDisplayname(player);
        }
    }
    
    private void refreshDisplayname(Player player) {
        if(!chatConfig.getBoolean("use-custom-displayname")) return;
        
        var placeholders = new Placeholder[] {
            new Placeholder("prefix", chat.getPlayerPrefix(player)),
            new Placeholder("player", player.getName()),
            new Placeholder("surfix", chat.getPlayerSuffix(player))
        };
        
        var displayname = chatConfig.getString("displayname-format", null);
        if(displayname != null){
            for (Placeholder placeholder : placeholders) {
                displayname = displayname.replace("${" + placeholder.key + "}", placeholder.value);
            }
            
            player.setDisplayName(ChatUtils.hexColor(displayname));
        }
    }
    
    @EventHandler
    @SuppressWarnings("unused")
    private void onPlayerJoin(PlayerJoinEvent event){
        refreshDisplayname(event.getPlayer());
        if(!chatConfig.getBoolean("use-custom-join-message")) return;
        event.setJoinMessage(null);
        
        var ph_displayname = new Placeholder("displayname", event.getPlayer().getDisplayName());
        var ph_name = new Placeholder("name", event.getPlayer().getName());
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if(player==null)continue;
            var locale = player.getLocale();
            var message = langManager.getMessage("chat.player_join", locale, ph_displayname, ph_name);
            player.sendMessage(message);
        }
    }
    
    @EventHandler
    @SuppressWarnings("unused")
    private void onPlayerQuit(PlayerQuitEvent event){
        refreshDisplayname(event.getPlayer());
        if(!chatConfig.getBoolean("use-custom-leave-message")) return;
        event.setQuitMessage(null);
        
        var ph_displayname = new Placeholder("displayname", event.getPlayer().getDisplayName());
        var ph_name = new Placeholder("name", event.getPlayer().getName());
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if(player==null)continue;
            var locale = player.getLocale();
            var message = langManager.getMessage("chat.player_leave", locale, ph_displayname, ph_name);
            player.sendMessage(message);
        }
    }
    
    @EventHandler
    @SuppressWarnings("unused")
    private void onChatMessage(AsyncPlayerChatEvent event){
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if(player==null)continue;
            refreshDisplayname(player);
        }
    }
    
    @EventHandler
    @SuppressWarnings("unused")
    private void onChatMessage(PlayerCommandPreprocessEvent event){
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if(player==null)continue;
            refreshDisplayname(player);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return null;
    }

    
}
