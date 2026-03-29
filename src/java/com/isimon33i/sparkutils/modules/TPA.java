package com.isimon33i.sparkutils.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ChatUtils;
import com.isimon33i.utils.lang.Placeholder;

public class TPA extends Module implements Runnable {

    public TPA(Main plugin) {
        super(plugin);
    }

    private class TPRequest {

        public UUID playerID;
        public String playerName;
        public long requestTime;
        public boolean here;

        public TPRequest(UUID playerID, String playerName, long requestTime, boolean here) {
            this.playerID = playerID;
            this.playerName = playerName;
            this.requestTime = requestTime;
            this.here = here;
        }

        public TPRequest(Player player, boolean here) {
            this(player.getUniqueId(), player.getName(), System.currentTimeMillis(), here);
        }

        public Player GetPlayer() {
            return Bukkit.getPlayer(playerID);
        }
    }
    
    private int taskId = -1;
    private HashMap<UUID, List<TPRequest>> requests = new HashMap<>();
    private HashSet<UUID> tpaDisabled = new HashSet<>();
    private long requestTimeout = 30000;

    @Override
    public void onRegister() {
        registerCommand(plugin, "tpa", this);
        registerCommand(plugin, "tpahere", this);
        registerCommand(plugin, "tpaccept", this);
        registerCommand(plugin, "tpdeny", this);
        registerCommand(plugin, "tpatoggle", this);

        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 20, 20);
    }

    @Override
    public void onUnregister() {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }

    @Override
    public void run() {

        for (var entry : requests.entrySet()) {
            var list = entry.getValue();
            if (list == null) {
                continue;
            }
            for (int i = list.size() - 1; i >= 0; i--) {
                var req = list.get(i);
                if (System.currentTimeMillis() - req.requestTime > requestTimeout) {
                    list.remove(i);
                    var dstPlayer = Bukkit.getPlayer(entry.getKey());
                    var srcPlayer = Bukkit.getPlayer(req.playerID);
                    if(dstPlayer != null && srcPlayer != null) {
                        var srcPlayerPlaceholder = new Placeholder("srcPlayer", srcPlayer.getDisplayName());
                        var dstPlayerPlaceholder = new Placeholder("dstPlayer", dstPlayer.getDisplayName());
                        srcPlayer.sendMessage(langManager.getMessage("tpa.request.timeout.to", srcPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder));
                        dstPlayer.sendMessage(langManager.getMessage("tpa.request.timeout.from", dstPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder));
                    }
                }
            }
        }
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
        
        if (player != null) {
            var srcPlayerPlaceholder = new Placeholder("srcPlayer", srcDisplayName);

            if (command.getName().equalsIgnoreCase("tpa")) {
                if (!player.hasPermission("sparkutils.tpa")) {
                    player.sendMessage(langManager.getMessage("tpa.permission_denied", locale));
                    return true;
                }
                if (args.length == 1) {
                    var targetPlayer = sender.getServer().getPlayer(args[0]);
                    final Placeholder dstPlayerPlaceholder;
                    if(targetPlayer == null){
                        dstPlayerPlaceholder = new Placeholder("dstPlayer", args[0]);
                        player.sendMessage(langManager.getMessage("tpa.player_not_found", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                        return true;
                    }else{
                        dstPlayerPlaceholder = new Placeholder("dstPlayer", targetPlayer.getDisplayName());
                    }
                    if (tpaDisabled.contains(targetPlayer.getUniqueId())) {
                        player.sendMessage(langManager.getMessage("tpa.request.target_disabled", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                        return true;
                    }

                    var requestList = requests.get(targetPlayer.getUniqueId());
                    if (requestList == null) {
                        requestList = new ArrayList<>();
                        requests.put(targetPlayer.getUniqueId(), requestList);
                    }
                    for (var req : requestList) {
                        if (req.playerID == player.getUniqueId()) {
                            player.sendMessage(langManager.getMessage("tpa.request.already_pending", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                            return true;
                        }
                    }
                    requestList.add(new TPRequest(player, false));
                    player.sendMessage(langManager.getMessage("tpa.request.send", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                    ChatUtils.sendTwoButtonMessage(
                            targetPlayer,
                            langManager.getMessage("tpa.request.received", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            langManager.getMessage("tpa.accept_button", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            langManager.getMessage("tpa.deny_button", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            "tpaccept",
                            "tpdeny"
                    );
                    return true;
                }
            }

            if (command.getName().equalsIgnoreCase("tpahere")) {
                if (!player.hasPermission("sparkutils.tpa.here")) {
                    player.sendMessage(langManager.getMessage("tpa.permission_denied", locale));
                    return true;
                }
                if (args.length == 1) {
                    var targetPlayer = sender.getServer().getPlayer(args[0]);
                    final Placeholder dstPlayerPlaceholder;
                    if(targetPlayer == null){
                        dstPlayerPlaceholder = new Placeholder("dstPlayer", args[0]);
                        player.sendMessage(langManager.getMessage("tpa.player_not_found", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                        return true;
                    }else{
                        dstPlayerPlaceholder = new Placeholder("dstPlayer", targetPlayer.getDisplayName());
                    }
                    if (tpaDisabled.contains(targetPlayer.getUniqueId())) {
                        player.sendMessage(langManager.getMessage("tpa.request.target_disabled", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                        return true;
                    }

                    var requestList = requests.get(targetPlayer.getUniqueId());
                    if (requestList == null) {
                        requestList = new ArrayList<>();
                        requests.put(targetPlayer.getUniqueId(), requestList);
                    }
                    for (var req : requestList) {
                        if (req.playerID == player.getUniqueId()) {
                            player.sendMessage(langManager.getMessage("tpa.request.already_pending", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                            return true;
                        }
                    }
                    requestList.add(new TPRequest(player, true));
                    //player.sendMessage(ChatColor.GOLD + "TP here request sent to player " + ChatColor.DARK_GREEN + ChatColor.BOLD + targetPlayer.getDisplayName());
                    //ChatUtils.sendTwoButtonMessage(targetPlayer, ChatColor.GOLD + "You have received a TP here request from " + ChatColor.DARK_GREEN + ChatColor.BOLD + player.getDisplayName(), "[Accept]", "[Deny]", "tpaccept", "tpdeny");

                    player.sendMessage(langManager.getMessage("tpa.request.send.here", locale, srcPlayerPlaceholder, dstPlayerPlaceholder));
                    ChatUtils.sendTwoButtonMessage(
                            targetPlayer,
                            langManager.getMessage("tpa.request.received.here", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            langManager.getMessage("tpa.accept_button", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            langManager.getMessage("tpa.deny_button", targetPlayer.getLocale(), srcPlayerPlaceholder, dstPlayerPlaceholder),
                            "tpaccept",
                            "tpdeny"
                    );

                    return true;
                }
            }

            if (command.getName().equalsIgnoreCase("tpaccept")) {
                List<TPRequest> requestList = requests.get(player.getUniqueId());
                if (requestList != null) {
                    if (args.length == 1) {

                        for (var req : requestList) {
                            if (req.playerName.equalsIgnoreCase(args[0])) {
                                Teleport(req.GetPlayer(), player);
                                return true;
                            }
                        }
                        //player.sendMessage(ChatColor.RED + "No pending request from player " + ChatColor.DARK_GREEN + ChatColor.BOLD + args[0]);
                        player.sendMessage(langManager.getMessage("tpa.request.no_pending_from_player", locale, new Placeholder("srcPlayer", args[0])));
                        return true;
                    } else if (args.length == 0) {
                        if (requestList.isEmpty()) {
                            //player.sendMessage(ChatColor.RED + "No requests pending");
                            player.sendMessage(langManager.getMessage("tpa.request.no_pending", locale));
                            return true;
                        } else {
                            var req = requestList.remove(0);
                            if (req.here) {
                                Teleport(player, req.GetPlayer());
                            } else {
                                Teleport(req.GetPlayer(), player);
                            }
                            return true;
                        }
                    }
                }
            }

            if (command.getName().equalsIgnoreCase("tpdeny")) {
                List<TPRequest> requestList = requests.get(player.getUniqueId());
                if (requestList != null) {
                    if (args.length == 1) {
                        for (var req : requestList) {
                            if (req.playerName.equalsIgnoreCase(args[0])) {
                                requestList.remove(req);
                                player.sendMessage(langManager.getMessage("tpa.request.denied", locale, new Placeholder("srcPlayer", args[0])));
                                return true;
                            }
                        }
                        //player.sendMessage(ChatColor.RED + "No pending request from player " + ChatColor.DARK_GREEN + ChatColor.BOLD + args[0]);
                        player.sendMessage(langManager.getMessage("tpa.request.no_pending_from_player", locale, new Placeholder("srcPlayer", args[0])));
                        return true;
                    } else if (args.length == 0) {
                        if (requestList.isEmpty()) {
                            //player.sendMessage(ChatColor.RED + "No requests pending");
                            player.sendMessage(langManager.getMessage("tpa.request.no_pending", locale));
                            return true;
                        } else {
                            var req = requestList.remove(0);
                            player.sendMessage(langManager.getMessage("tpa.request.denied", locale, new Placeholder("srcPlayer", req.playerName)));
                            return true;
                        }
                    }
                }
            }

            if (command.getName().equalsIgnoreCase("tpatoggle")) {
                if (!player.hasPermission("sparkutils.tpa.toggle")) {
                    player.sendMessage(langManager.getMessage("tpa.permission_denied", locale));
                    return true;
                }
                var playerID = player.getUniqueId();
                var tpa = tpaDisabled.contains(playerID);
                if (tpa) {
                    tpaDisabled.remove(playerID);
                } else {
                    tpaDisabled.add(playerID);
                }
                //player.sendMessage(ChatColor.GOLD + "TPA " + ChatColor.DARK_GREEN + ChatColor.BOLD + (!tpa ? "Disabled" : "Enabled"));
                var enabled = langManager.getMessage("tpa.enabled", locale);
                var disabled = langManager.getMessage("tpa.disabled", locale);
                player.sendMessage(langManager.getMessage("tpa.toggled", locale, srcPlayerPlaceholder, new Placeholder("state", tpa ? enabled : disabled)));
                return true;
            }

        } else {
            sender.sendMessage(langManager.getMessage("tpa.console_cant_tpa", locale));
            return true;
        }
        return false;
    }

    private void Teleport(Player srcPlayer, Player dstPlayer) {
        srcPlayer.teleport(dstPlayer);
        //srcPlayer.sendMessage(ChatColor.GREEN + "You have been teleported to player " + ChatColor.DARK_GREEN + ChatColor.BOLD + dstPlayer.getDisplayName());
        //dstPlayer.sendMessage(ChatColor.GREEN + "Player " + ChatColor.DARK_GREEN + ChatColor.BOLD + srcPlayer.getDisplayName() + ChatColor.GREEN + " has been teleported to your position");
        srcPlayer.sendMessage(langManager.getMessage("tpa.teleported.you", srcPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer.getDisplayName()), new Placeholder("dstPlayer", dstPlayer.getDisplayName())));
        dstPlayer.sendMessage(langManager.getMessage("tpa.teleported.other", dstPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer.getDisplayName()), new Placeholder("dstPlayer", dstPlayer.getDisplayName())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return null;
    }

}
