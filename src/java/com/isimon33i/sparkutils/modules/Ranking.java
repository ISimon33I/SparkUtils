package com.isimon33i.sparkutils.modules;

import java.util.List;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ConfigUtils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

public class Ranking extends Module implements Runnable {
    
    LuckPerms luckPermsAPI;
    
    private TreeMap<Long, String> trackMap;
    
    FileConfiguration rankingConfig;
    final String rankingConfigFilePath = "ranking.yml";
    
    public Ranking(Main plugin) {
        super(plugin);
    }

    @Override
    public void onRegister() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPermsAPI = provider.getProvider();
        }
        
        var promoteTrackSection = rankingConfig.getConfigurationSection("rank.promote-track");
        trackMap = new TreeMap<>();
        for (String key : promoteTrackSection.getKeys(false)) {
            trackMap.put(promoteTrackSection.getLong(key), key);
        }
        
        rankingConfig = ConfigUtils.createConfig(plugin, rankingConfigFilePath, true, false);

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 20);
    }

    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        return null;
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            var uuid = player.getUniqueId();
            var state = plugin.getUtilities().getPlayerState(uuid);
            // TODO Fix ranking by playtime
            if (luckPermsAPI != null) {
                User user = luckPermsAPI.getUserManager().getUser(uuid);
                Node groupNode = null;
                for (var node : user.data().toCollection()) {
                    for (var set : trackMap.entrySet()) {
                        if (node.getKey().equalsIgnoreCase("group." + set.getValue())) {
                            groupNode = node;
                            break;
                        }
                    }
                    if (groupNode != null) {
                        break;
                    }
                }

                if (groupNode != null) {
                    if (state.playtimeCounterActive) {
                        plugin.getUtilities().stopPlaytimeCounter(uuid);
                        plugin.getUtilities().startPlaytimeCounter(uuid);
                    }

                    for (var set : trackMap.descendingMap().entrySet()) {
                        if (set.getKey() <= state.playtime) {
                            if (!set.getValue().equalsIgnoreCase(groupNode.getKey())) {
                                user.data().remove(groupNode);
                                user.data().add(Node.builder("group." + set.getValue()).build());
                                luckPermsAPI.getUserManager().saveUser(user);
                            }
                            break;
                        }
                    }
                }

            }
        }
    }

}
