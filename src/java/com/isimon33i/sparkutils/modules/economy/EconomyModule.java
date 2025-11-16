package com.isimon33i.sparkutils.modules.economy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import com.isimon33i.sparkutils.modules.Module;
import com.isimon33i.utils.ChatUtils;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.lang.Placeholder;

import net.milkbowl.vault.economy.Economy;

public class EconomyModule extends Module implements Listener, Runnable {

    final String economyConfigFilePath = "economy.yml";
    FileConfiguration economyConfig;

    EconomyBackend economyBackend;
    EconomyVault economyVault;

    public record PlayerPayRequest(UUID srcPlayer, double amount, long millisOnCreation) {

    }

    public record PlayerPayConfirm(UUID dstPlayer, double amount, long millisOnCreation) {

    }
    private int taskId = -1;
    private HashMap<UUID, List<PlayerPayRequest>> payRequests = new HashMap<>();
    private HashMap<UUID, List<PlayerPayConfirm>> payConfirmes = new HashMap<>();

    public EconomyModule(JavaPlugin plugin) {
        super(plugin);

    }

    @Override
    public void onRegister(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            economyConfig = ConfigUtils.createConfig(plugin, economyConfigFilePath, false);

            var storageSection = economyConfig.getConfigurationSection("storage");
            if (storageSection != null) {
                var backendName = storageSection.getString("backend", "sqlite");
                if (backendName != null) {
                    switch (backendName.toLowerCase()) {
                        case "sqlite" -> {
                            try {
                                economyBackend = new EconomyBackendSQLite(plugin.getDataFolder().getAbsolutePath() + "/" + storageSection.getString("path", "economy.db"));
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        default -> {
                            plugin.getLogger().log(Level.SEVERE, "Unknown economy storage backend! Backend: {0}", backendName);
                        }
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Economy storage backend field missing from config!");
                }
            } else {
                plugin.getLogger().log(Level.SEVERE, "Economy storage section missing from config!");
            }
            if (economyBackend != null) {
                economyVault = new EconomyVault(this, economyBackend);
                plugin.getServer().getServicesManager().register(Economy.class, economyVault, plugin, ServicePriority.High);
            } else {
                plugin.getLogger().log(Level.SEVERE, "Economy disabled!");
                return;
            }

            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            for (var player : plugin.getServer().getOnlinePlayers()) {
                createAccount(player);
            }

            registerCommand(plugin, "balance", this);
            registerCommand(plugin, "setbalance", this);
            registerCommand(plugin, "pay", this);
            registerCommand(plugin, "payrequest", this);
            registerCommand(plugin, "payaccept", this);
            registerCommand(plugin, "paydeny", this);
            registerCommand(plugin, "payconfirm", this);
            registerCommand(plugin, "paycancle", this);
            registerCommand(plugin, "top", this);

            taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 20, 20);

            plugin.getLogger().log(Level.WARNING, "Economy enabled");
        } else {
            plugin.getLogger().log(Level.WARNING, "Vault missing! Economy disabled");
        }
    }

    @Override
    public void onUnregister(JavaPlugin plugin) {
        plugin.getServer().getScheduler().cancelTask(taskId);
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onPlayerJoin(PlayerJoinEvent e) {
        createAccount(e.getPlayer());
    }

    private void createAccount(Player player) {
        if (!economyBackend.hasAccount(player.getUniqueId())) {
            economyBackend.createAccount(player.getUniqueId());
            plugin.getLogger().log(Level.INFO, "Creating economic account for player \"{0}\"", player.getDisplayName());
        }
    }

    private boolean tryToPay(UUID srcPlayerUUID, UUID dstPlayerUUID, double amount) {
        if (economyBackend.has(srcPlayerUUID, amount)) {
            economyBackend.withdrawPlayer(srcPlayerUUID, amount);
            economyBackend.depositPlayer(dstPlayerUUID, amount);
            var balance = economyBackend.getBalance(srcPlayerUUID);
            var targetBalance = economyBackend.getBalance(dstPlayerUUID);

            var srcPlayer = plugin.getServer().getPlayer(srcPlayerUUID);
            var dstPlayer = plugin.getServer().getPlayer(dstPlayerUUID);
            if (srcPlayer != null) {
                srcPlayer.sendMessage(langManager.getMessage("economy.pay", srcPlayer.getLocale(), new Placeholder("amount", economyVault.format(amount)), new Placeholder("balance", economyVault.format(balance)), new Placeholder("targetPlayer", dstPlayer.getDisplayName())));
            }
            if (dstPlayer != null) {
                dstPlayer.sendMessage(langManager.getMessage("economy.pay.receive", dstPlayer.getLocale(), new Placeholder("amount", economyVault.format(amount)), new Placeholder("balance", economyVault.format(targetBalance)), new Placeholder("srcPlayer", srcPlayer.getDisplayName())));
            }
            return true;
        } else {
            var balance = economyBackend.getBalance(srcPlayerUUID);
            var srcPlayer = plugin.getServer().getPlayer(srcPlayerUUID);
            if (srcPlayer != null) {
                srcPlayer.sendMessage(langManager.getMessage("economy.to_low_balance", srcPlayer.getLocale(), new Placeholder("balance", economyVault.format(balance))));
            }
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        if (sender == null) {
            return false;
        }
        Player player = null;
        String locale = "en_US";
        if (sender instanceof Player p) {
            player = p;
            locale = player.getLocale();
        }

        if (command.getName().equalsIgnoreCase("balance")) {
            switch (args.length) {
                case 0 -> {
                    if (sender.hasPermission("sparkutils.economy.balance")) {
                        if (player != null) {
                            var balance = economyVault.getBalance(player);
                            player.sendMessage(langManager.getMessage("economy.balance", locale, new Placeholder("balance", economyVault.format(balance))));
                            return true;
                        } else {
                            sender.sendMessage(langManager.getMessage("economy.console_balance", locale));
                            return true;
                        }
                    } else {
                        sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                        return true;
                    }
                }
                case 1 -> {
                    if (sender.hasPermission("sparkutils.economy.balance.other")) {
                        player = plugin.getServer().getPlayer(args[0]);
                        if (player != null) {
                            var balance = economyVault.getBalance(player);
                            sender.sendMessage(langManager.getMessage("economy.balance", locale, new Placeholder("balance", economyVault.format(balance))));
                            return true;
                        } else {
                            sender.sendMessage(langManager.getMessage("core.player_not_found", locale, new Placeholder("player", args[0])));
                            return true;
                        }
                    } else {
                        sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                        return true;
                    }
                }
                default -> {
                    sender.sendMessage(langManager.getMessage("economy.command_help.balance", locale));
                    return true;
                }
            }
        } else if (command.getName().equalsIgnoreCase("setbalance")) {
            if (args.length == 2) {
                if (sender.hasPermission("sparkutils.economy.setbalance")) {
                    player = plugin.getServer().getPlayer(args[0]);
                    if (player != null) {
                        try {
                            var balance = Double.parseDouble(args[1]);
                            economyBackend.setBalance(player.getUniqueId(), balance);
                            sender.sendMessage(langManager.getMessage("economy.setbalance", locale, new Placeholder("balance", economyVault.format(balance))));
                            return true;
                        } catch (NumberFormatException e) {
                            sender.sendMessage(langManager.getMessage("core.cant_parse_number", locale, new Placeholder("input", args[1])));
                            return true;
                        }
                    } else {
                        sender.sendMessage(langManager.getMessage("core.player_not_found", locale, new Placeholder("player", args[0])));
                        return true;
                    }
                } else {
                    sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                    return true;
                }
            } else {
                sender.sendMessage(langManager.getMessage("economy.command_help.setbalance", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length == 2) {
                if (sender.hasPermission("sparkutils.economy.pay")) {
                    if (player != null) {
                        var targetPlayer = plugin.getServer().getPlayer(args[0]);
                        if (targetPlayer != null) {
                            try {
                                var amount = Double.parseDouble(args[1]);
                                var requests = payConfirmes.get(player.getUniqueId());
                                if (requests == null) {
                                    requests = new ArrayList<>();
                                    payConfirmes.put(player.getUniqueId(), requests);
                                }
                                requests.add(new PlayerPayConfirm(targetPlayer.getUniqueId(), amount, System.currentTimeMillis()));

                                ChatUtils.sendTwoButtonMessage(
                                        player,
                                        langManager.getMessage("economy.payconfirm_notice", locale, new Placeholder("targetPlayer", targetPlayer.getDisplayName()), new Placeholder("amount", economyVault.format(amount))),
                                        langManager.getMessage("core.confirm", locale),
                                        langManager.getMessage("core.cancle", locale),
                                        "payconfirm",
                                        "paycancle"
                                );
                            } catch (NumberFormatException e) {
                                sender.sendMessage(langManager.getMessage("core.cant_parse_number", locale, new Placeholder("input", args[1])));
                                return true;
                            }
                        } else {
                            sender.sendMessage(langManager.getMessage("core.player_not_found", locale, new Placeholder("player", args[0])));
                            return true;
                        }
                    } else {
                        sender.sendMessage(langManager.getMessage("economy.console_cant_pay", locale));
                        return true;
                    }
                } else {
                    sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                    return true;
                }
            } else {
                sender.sendMessage(langManager.getMessage("economy.command_help.pay", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("top")) {
            if (sender.hasPermission("sparkutils.economy.top")) {
                int page = 0;
                if (args.length > 0) {
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(langManager.getMessage("core.cant_parse_number", locale, new Placeholder("input", args[1])));
                        return true;
                    }
                }
                int count = 5;
                if (args.length > 1) {
                    try {
                        count = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(langManager.getMessage("core.cant_parse_number", locale, new Placeholder("input", args[1])));
                        return true;
                    }
                }
                var balances = economyBackend.getBalances();
                var startIndex = page * count;
                sender.sendMessage(langManager.getMessage("economy.top.title", locale));
                for (int i = startIndex; i < startIndex + count && i < balances.length; i++) {
                    var balance = balances[i];
                    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(balance.uuid());
                    sender.sendMessage(langManager.getMessage("economy.top.entry", locale, new Placeholder("index", Integer.toString(i)), new Placeholder("player", offlinePlayer.getName()), new Placeholder("balance", economyVault.format(balance.balance()))));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("payrequest")) {
            if (sender.hasPermission("sparkutils.economy.payrequest")) {
                if (args.length == 2) {
                    if (player != null) {
                        var targetPlayer = plugin.getServer().getPlayer(args[0]);
                        if (targetPlayer != null) {
                            var requests = payRequests.get(targetPlayer.getUniqueId());
                            if (requests == null) {
                                requests = new ArrayList<>();
                                payRequests.put(targetPlayer.getUniqueId(), requests);
                            }
                            try {
                                var amount = Double.parseDouble(args[1]);
                                requests.add(new PlayerPayRequest(player.getUniqueId(), amount, System.currentTimeMillis()));
                                player.sendMessage(langManager.getMessage("economy.payrequest.send", locale, new Placeholder("targetPlayer", targetPlayer.getDisplayName()), new Placeholder("amount", economyVault.format(amount))));
                                ChatUtils.sendTwoButtonMessage(
                                        targetPlayer,
                                        langManager.getMessage("economy.payrequest_notice", locale, new Placeholder("srcPlayer", player.getDisplayName()), new Placeholder("amount", economyVault.format(amount))),
                                        langManager.getMessage("core.accept", locale),
                                        langManager.getMessage("core.deny", locale),
                                        "payaccept",
                                        "paydeny"
                                );
                            } catch (NumberFormatException e) {
                                sender.sendMessage(langManager.getMessage("core.cant_parse_number", locale, new Placeholder("input", args[1])));
                                return true;
                            }
                        } else {
                            sender.sendMessage(langManager.getMessage("core.player_not_found", locale, new Placeholder("player", args[0])));
                            return true;
                        }
                    } else {
                        sender.sendMessage(langManager.getMessage("economy.console_cant_request", locale));
                        return true;
                    }
                } else {
                    sender.sendMessage(langManager.getMessage("economy.command_help.payrequest", locale));
                    return true;
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("payconfirm")) {
            if (player != null) {
                var requests = payConfirmes.get(player.getUniqueId());
                if (requests != null && !requests.isEmpty()) {
                    var request = requests.getFirst();
                    requests.remove(0);
                    tryToPay(player.getUniqueId(), request.dstPlayer, request.amount);
                }else{
                    player.sendMessage(langManager.getMessage("economy.payconfirm.nothing_to_confirm", locale));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("paycancle")) {
            if(player!=null){
                var requests = payConfirmes.get(player.getUniqueId());
                if (requests != null && !requests.isEmpty()) {
                    var request = requests.getFirst();
                    requests.remove(0);
                    var dstPlayer = plugin.getServer().getPlayer(request.dstPlayer);
                    if (dstPlayer != null) {
                        player.sendMessage(langManager.getMessage("economy.payconfirm.cancled", locale, new Placeholder("targetPlayer", dstPlayer.getDisplayName()), new Placeholder("amount", economyVault.format(request.amount))));
                    } else {
                        player.sendMessage(langManager.getMessage("economy.payconfirm.cancled_short", locale));
                    }
                }else{
                    player.sendMessage(langManager.getMessage("economy.payconfirm.nothing_to_cancle", locale));
                }    
            } else {
                sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("payaccept")) {
            if(player!=null){
                var requests = payRequests.get(player.getUniqueId());
                if (requests != null && !requests.isEmpty()) {
                    var request = requests.getFirst();
                    requests.remove(0);
                    tryToPay(player.getUniqueId(), request.srcPlayer, request.amount);
                }else{
                    player.sendMessage(langManager.getMessage("economy.payrequest.nothing_to_accept", locale));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("paydeny")) {
            if (player != null) {
                var requests = payRequests.get(player.getUniqueId());
                if (requests != null && !requests.isEmpty()) {
                    var request = requests.getFirst();
                    requests.remove(0);
                    var srcPlayer = plugin.getServer().getPlayer(request.srcPlayer);
                    if (srcPlayer != null) {
                        player.sendMessage(langManager.getMessage("economy.payrequest.denied", locale, new Placeholder("srcPlayer", srcPlayer.getDisplayName()), new Placeholder("amount", economyVault.format(request.amount))));
                    } else {
                        player.sendMessage(langManager.getMessage("economy.payrequest.denied_short", locale));
                    }
                }else{
                    player.sendMessage(langManager.getMessage("economy.payrequest.nothing_to_deny", locale));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String string, String[] args) {
        return null;
    }

    @Override
    public void run() {
        int request_timeout = economyConfig.getInt("request_timout", 10000);
        int confirm_timeout = economyConfig.getInt("confirm_timout", 10000);

        for (var requests_set : payRequests.entrySet()) {
            var dstPlayerUUID = requests_set.getKey();
            var dstPlayer = plugin.getServer().getPlayer(dstPlayerUUID);
            var requests = requests_set.getValue();
            for (int i = requests.size()-1; i >= 0; i--) {
                var request = requests.get(i);
                var srcPlayerUUID = request.srcPlayer();
                var srcPlayer = plugin.getServer().getPlayer(srcPlayerUUID);
                var requestAge = System.currentTimeMillis() - request.millisOnCreation;
                if (requestAge > request_timeout) {
                    requests.remove(i);
                    if (srcPlayer != null) {
                        srcPlayer.sendMessage(langManager.getMessage("economy.request_timeout", srcPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer.getDisplayName()), new Placeholder("dstPlayer", dstPlayer != null ? dstPlayer.getDisplayName() : "Unknown player")));
                    }
                    if (dstPlayer != null) {
                        dstPlayer.sendMessage(langManager.getMessage("economy.request_timeout", dstPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer != null ? srcPlayer.getDisplayName() : "Unknown player"), new Placeholder("dstPlayer", dstPlayer.getDisplayName())));
                    }
                }
            }
        }

        for (var requests_set : payConfirmes.entrySet()) {
            var srcPlayerUUID = requests_set.getKey();
            var srcPlayer = plugin.getServer().getPlayer(srcPlayerUUID);
            var requests = requests_set.getValue();
            for (int i = requests.size()-1; i >= 0; i--) {
                var request = requests.get(i);
                var dstPlayerUUID = request.dstPlayer();
                var dstPlayer = plugin.getServer().getPlayer(dstPlayerUUID);
                var requestAge = System.currentTimeMillis() - request.millisOnCreation;
                if (requestAge > confirm_timeout) {
                    requests.remove(i);
                    if (srcPlayer != null) {
                        srcPlayer.sendMessage(langManager.getMessage("economy.confirm_timeout", srcPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer.getDisplayName()), new Placeholder("dstPlayer", dstPlayer != null ? dstPlayer.getDisplayName() : "Unknown player")));
                    }
                    if (dstPlayer != null) {
                        dstPlayer.sendMessage(langManager.getMessage("economy.confirm_timeout", dstPlayer.getLocale(), new Placeholder("srcPlayer", srcPlayer != null ? srcPlayer.getDisplayName() : "Unknown player"), new Placeholder("dstPlayer", dstPlayer.getDisplayName())));
                    }
                }
            }
        }
    }
}
