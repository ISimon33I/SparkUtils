package com.isimon33i.sparkutils.modules.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.sparkutils.modules.Module;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.Utils;
import com.isimon33i.utils.lang.Placeholder;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class UtilitiesModule extends Module implements Listener, Runnable {

    FileConfiguration utilitiesConfig;
    final String utilitiesConfigFilePath = "utilities.yml";

    private Map<UUID, PlayerState> playerStates;
    

    public PlayerState getPlayerState(UUID uuid) {
        PlayerState state;
        if (!playerStates.containsKey(uuid)) {
            state = PlayerState.load(plugin, uuid);
            playerStates.put(uuid, state);
        } else {
            state = playerStates.get(uuid);
        }
        return state;
    }

    public UtilitiesModule(Main plugin) {
        super(plugin);
    }

    @Override
    public void onRegister() {
        playerStates = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        utilitiesConfig = ConfigUtils.createConfig(plugin, utilitiesConfigFilePath, true, false);

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 20);

        registerCommand(plugin, "afk", this);
        registerCommand(plugin, "playtime", this);
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            startPlaytimeCounter(player.getUniqueId());
            playerDidSomthing(player.getUniqueId());
        }
    }

    @Override
    public void onUnregister() {
        for (var uuid : playerStates.keySet()) {
            PlayerState.save(plugin, uuid, playerStates.get(uuid));
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent e) {
        startPlaytimeCounter(e.getPlayer().getUniqueId());
        playerDidSomthing(e.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        var uuid = e.getPlayer().getUniqueId();
        stopPlaytimeCounter(uuid);
        PlayerState.save(plugin, uuid, playerStates.get(uuid));
        playerStates.remove(uuid);
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent e) {
        playerDidSomthing(e.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent e) {
        playerDidSomthing(e.getPlayer().getUniqueId());
    }

    public enum MessageStyle {
        Chat,
        Title,
        ActionBar
    }

    public static void sendMessageToPlayer(Player player, @Nullable String message, @Nullable String subTitle, MessageStyle style, int fadeIn, int stay, int fadeOut) {
        if (null != style) {
            switch (style) {
                case Chat -> {
                    if (message != null) {
                        player.sendMessage(message);
                    }
                }
                case Title ->
                    player.sendTitle(message, subTitle, fadeIn, stay, fadeOut);
                case ActionBar ->
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
                default -> {
                }
            }
        }
    }

    public MessageStyle getMessageStyle() {
        var styleString = utilitiesConfig.getString("afk.self-message-style", "chat");
        if (styleString.equalsIgnoreCase("chat")) {
            return MessageStyle.Chat;
        } else if (styleString.equalsIgnoreCase("title")) {
            return MessageStyle.Title;
        } else if (styleString.equalsIgnoreCase("actionbar")) {
            return MessageStyle.ActionBar;
        } else {
            return MessageStyle.Chat;
        }
    }

    public boolean isAFK(UUID uuid) {
        var state = getPlayerState(uuid);
        return state.afk;
    }

    public void setAFK(UUID uuid, boolean afk) {
        var state = getPlayerState(uuid);
        boolean afkChanged = false;
        if (state.afk && !afk) {
            startPlaytimeCounter(uuid);
            afkChanged = true;
        } else if (!state.afk && afk) {
            if (utilitiesConfig.getBoolean("afk.pause-playtime-when-afk")) {
                stopPlaytimeCounter(uuid);
            }
            afkChanged = true;
        }
        state.afk = afk;

        Player thisPlayer = plugin.getServer().getPlayer(uuid);

        if (afkChanged) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                var playerPlaceholder = new Placeholder("player", thisPlayer.getDisplayName());
                if (player == thisPlayer) {
                    if (utilitiesConfig.getBoolean("afk.send-message-to-self")) {
                        var style = getMessageStyle();
                        int fadeIn = (int) Math.round(utilitiesConfig.getDouble("afk.title-fadeIn") * 20);
                        int stay = (int) Math.round(utilitiesConfig.getDouble("afk.title-stay") * 20);
                        int fadeOut = (int) Math.round(utilitiesConfig.getDouble("afk.title-fadeOut") * 20);

                        if (afk) {
                            sendMessageToPlayer(thisPlayer, langManager.getMessage("utilities.afk.true.self", thisPlayer.getLocale(), playerPlaceholder), null, style, fadeIn, stay, fadeOut);
                        } else {
                            sendMessageToPlayer(thisPlayer, langManager.getMessage("utilities.afk.false.self", thisPlayer.getLocale(), playerPlaceholder), null, style, fadeIn, stay, fadeOut);
                        }
                    }
                } else {
                    if (utilitiesConfig.getBoolean("afk.send-message-to-others")) {
                        if (afk) {
                            player.sendMessage(langManager.getMessage("utilities.afk.true.other", thisPlayer.getLocale(), playerPlaceholder));
                        } else {
                            player.sendMessage(langManager.getMessage("utilities.afk.false.other", thisPlayer.getLocale(), playerPlaceholder));
                        }
                    }
                }
            }
        }
    }

    public void playerDidSomthing(UUID uuid) {
        var state = getPlayerState(uuid);
        state.lastAction = System.currentTimeMillis();
        setAFK(uuid, false);
    }

    public void stopPlaytimeCounter(UUID uuid) {
        var state = getPlayerState(uuid);
        if (state.playtimeCounterActive) {
            var timeNow = System.currentTimeMillis();
            state.playtime += timeNow - state.playtimeSegmentStart;
            state.playtimeCounterActive = false;
        }
    }

    public void startPlaytimeCounter(UUID uuid) {
        var state = getPlayerState(uuid);
        if (!state.playtimeCounterActive) {
            state.playtimeSegmentStart = System.currentTimeMillis();
            state.playtimeCounterActive = true;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return false;
        }
        final Player player;
        final String srcDisplayName;
        final String locale;
        if (sender instanceof Player p) {
            player = p;
            locale = player.getLocale();
            srcDisplayName = player.getDisplayName();
        } else {
            player = null;
            locale = "en_US";
            srcDisplayName = langManager.getMessage("console_display_name", locale);
        }

        if (command.getName().equalsIgnoreCase("afk")) {
            if (sender.hasPermission("sparkutils.afk")) {
                if (player != null) {
                    setAFK(player.getUniqueId(), !isAFK(player.getUniqueId()));
                } else {
                    sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("playtime")) {
            if (sender.hasPermission("sparkutils.playtime")) {
                if (player != null) {
                    stopPlaytimeCounter(player.getUniqueId());
                    var state = getPlayerState(player.getUniqueId());
                    var splittedTime = Utils.splitMillis(state.playtime);
                    var ph_d = new Placeholder("d", String.valueOf(splittedTime.days()));
                    var ph_h = new Placeholder("h", String.valueOf(splittedTime.hours()));
                    var ph_m = new Placeholder("m", String.valueOf(splittedTime.minutes()));
                    var ph_s = new Placeholder("s", String.valueOf(splittedTime.seconds()));
                    player.sendMessage(langManager.getMessage("utilities.playtime", locale, ph_d, ph_h, ph_m, ph_s));
                    startPlaytimeCounter(player.getUniqueId());
                } else {
                    sender.sendMessage(langManager.getMessage("core.console_cant_use", locale));
                }
            } else {
                sender.sendMessage(langManager.getMessage("core.permission_denied", locale));
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return null;
    }

    @Override
    public void run() {
        var timeNow = System.currentTimeMillis();
        var timeoutMillis = utilitiesConfig.getDouble("afk.timeout") * 1000;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            var uuid = player.getUniqueId();
            var state = getPlayerState(uuid);
            if (timeoutMillis > 0) {
                if ((timeNow - state.lastAction) > timeoutMillis) {
                    setAFK(uuid, true);
                }
            }
        }
    }
}
