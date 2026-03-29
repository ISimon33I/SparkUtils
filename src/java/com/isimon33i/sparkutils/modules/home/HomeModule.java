package com.isimon33i.sparkutils.modules.home;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.sparkutils.modules.Module;
import com.isimon33i.utils.ChatUtils;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.InventoryMenu;
import static com.isimon33i.utils.Utils.filterByStart;
import com.isimon33i.utils.lang.Placeholder;

public class HomeModule extends Module {

    final String homesConfigFilePath = "homes.yml";
    FileConfiguration homesConfig;

    InventoryMenu homesMenu = new InventoryMenu();
    public final NamespacedKey homeNameKey;

    public HomeModule(Main plugin) {
        super(plugin);
        homeNameKey = new NamespacedKey(plugin, "home.tp");
    }

    @Override
    public void onRegister() {
        homesConfig = ConfigUtils.createConfig(plugin, homesConfigFilePath, true, false);

        registerCommand(plugin, "home", this);
        registerCommand(plugin, "sethome", this);
        registerCommand(plugin, "edithome", this);
        registerCommand(plugin, "homelimit", this);

        homesMenu.onMenuClick = x -> onMenuClick(x);
        homesMenu.onPopulateMenu = (x, y) -> populateMeny(x, y);
        homesMenu.initialize(plugin);
    }

    @Override
    public void onUnregister() {
        homesMenu.closeAllInventories(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
        Player player = null;
        String locale = "en_US";
        if (sender instanceof Player p) {
            player = p;
            locale = player.getLocale();
        }
        args = ChatUtils.parseQuotedArgs(args).toArray(String[]::new);
        if (player != null) {
            var srcPlayerPlaceholder = new Placeholder("srcPlayer", player.getDisplayName());
            if (command.getName().equalsIgnoreCase("home")) {
                if (args.length == 0) {
                    openHomeMenu(player);
                } else if (args.length == 1) {
                    var home = getHome(player, args[0]);
                    home.teleportEntity(player);
                    var homeNamePlaceholder = new Placeholder("home", args[0]);
                    player.sendMessage(langManager.getMessage("home.teleporting", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                }
                return true;
            } else if (command.getName().equalsIgnoreCase("sethome")) {
                var homeLimit = getMaxAllowedHomes(player);
                var homeCount = getHomes(player).size();
                if (homeCount >= homeLimit) {
                    player.sendMessage(langManager.getMessage("home.limit_reached", locale, srcPlayerPlaceholder));
                    return true;
                }
                if (args.length == 1) {
                    var homeName = args[0];
                    var homeNamePlaceholder = new Placeholder("home", homeName);
                    if (homeExists(player, homeName)) {
                        player.sendMessage(langManager.getMessage("home.already_exists", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                        return true;
                    }
                    var home = new Home();
                    home.location = player.getLocation();
                    setHome(player, homeName, home);
                    player.sendMessage(langManager.getMessage("home.created", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                    return true;
                }
            } else if (command.getName().equalsIgnoreCase("edithome")) {
                if (args.length >= 2) {
                    var homeName = args[0];
                    var homeNamePlaceholder = new Placeholder("home", homeName);
                    if (!homeExists(player, homeName)) {
                        player.sendMessage(langManager.getMessage("home.not_found", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                        return true;
                    } else if (args[1].equalsIgnoreCase("remove")) {
                        removeHome(player, homeName);
                        player.sendMessage(langManager.getMessage("home.removed", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                        return true;
                    } else if (args[1].equalsIgnoreCase("move")) {
                        var home = getHome(player, homeName);
                        removeHome(player, homeName);
                        home.location = player.getLocation();
                        setHome(player, homeName, home);
                        player.sendMessage(langManager.getMessage("home.moved", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                        return true;
                    } else if (args[1].equalsIgnoreCase("rename")) {
                        if (args.length == 3) {
                            var newHomeName = args[2];
                            var home = getHome(player, homeName);
                            var newHomeNamePlaceholder = new Placeholder("home_new", newHomeName);
                            removeHome(player, homeName);
                            setHome(player, newHomeName, home);
                            player.sendMessage(langManager.getMessage("home.renamed", locale, srcPlayerPlaceholder, homeNamePlaceholder, newHomeNamePlaceholder));
                            return true;
                        } else {
                            player.sendMessage(langManager.getMessage("home.edithome_usage", locale, srcPlayerPlaceholder, homeNamePlaceholder));
                            return true;
                        }
                    } else if (args[1].equalsIgnoreCase("icon")) {
                        var iconName = args[2];
                        var home = getHome(player, homeName);
                        home.icon = Material.matchMaterial(iconName);
                        setHome(player, homeName, home);
                        var icon = home.icon.getKeyOrNull();
                        player.sendMessage(langManager.getMessage("home.icon_changed", locale, srcPlayerPlaceholder, new Placeholder("icon", icon != null ? icon.toString() : langManager.getMessage("home.no_icon", locale)), homeNamePlaceholder));
                        return true;
                    } else if (args[1].equalsIgnoreCase("displayname")) {
                        var home = getHome(player, homeName);
                        home.displayName = args.length == 3 ? args[2] : null;
                        setHome(player, homeName, home);
                        player.sendMessage(langManager.getMessage("home.displayname_changed", locale, srcPlayerPlaceholder, new Placeholder("displayname", home.displayName != null ? home.displayName : langManager.getMessage("home.no_displayname", locale)), homeNamePlaceholder));
                        return true;
                    } else if (args[1].equalsIgnoreCase("description")) {
                        var home = getHome(player, homeName);
                        home.description = args.length == 3 ? args[2] : null;
                        setHome(player, homeName, home);
                        player.sendMessage(langManager.getMessage("home.description_changed", locale, srcPlayerPlaceholder, new Placeholder("description", home.description != null ? home.description : langManager.getMessage("home.no_description", locale)), homeNamePlaceholder));
                        return true;
                    }

                }
            } else if (command.getName().equalsIgnoreCase("homelimit")) {
                player.sendMessage(langManager.getMessage("home.current_limit", locale, srcPlayerPlaceholder, new Placeholder("limit", String.valueOf(getMaxAllowedHomes(player)))));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String string, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("home") && args.length == 1) {
                return filterByStart(args[args.length - 1], getHomes(player), false);
            } else if (command.getName().equalsIgnoreCase("sethome") && args.length == 1) {
                return new ArrayList<>();
            } else if (command.getName().equalsIgnoreCase("edithome")) {
                switch (args.length) {
                    case 1 -> {
                        return filterByStart(args[args.length - 1], getHomes(player), false);
                    }
                    case 2 -> {
                        return filterByStart(args[args.length - 1], List.of("remove", "rename", "move", "icon", "displayname", "description"), false);
                    }
                    case 3 -> {
                        if (args[1].equalsIgnoreCase("icon")) {
                            return filterByStart(args[args.length - 1], Arrays.stream(Material.values()).map(Object::toString).toList(), false);
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private void onMenuClick(InventoryClickEvent event) {
        var item = event.getCurrentItem();
        if (item != null) {
            var meta = item.getItemMeta();
            if (meta != null) {
                var data = meta.getPersistentDataContainer();
                var warpName = data.get(homeNameKey, PersistentDataType.STRING);
                if (warpName != null) {
                    var playerEntity = event.getWhoClicked();
                    var player = (Player) playerEntity;
                    var home = getHome(playerEntity, warpName);
                    if (home.teleportEntity(playerEntity)) {
                        var homeNamePlaceholder = new Placeholder("home", (home.displayName!=null)?home.displayName:warpName);
                        playerEntity.sendMessage(langManager.getMessage("home.teleporting", player.getLocale(), new Placeholder("srcPlayer", player.getDisplayName()), homeNamePlaceholder));
                    }
                }
            }
        }
    }

    private void populateMeny(HumanEntity player, Inventory inventory) {
        for (var warpName : getHomes(player)) {
            var warp = getHome(player, warpName);

            var warpDisplayName = (warp.displayName == null || warp.displayName.isEmpty()) ? warpName : warp.displayName;
            var itemMaterial = warp.icon != null ? warp.icon : Material.GRASS_BLOCK;

            var item = new ItemStack(itemMaterial);
            var meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, warpName);

            meta.setDisplayName(ChatUtils.hexColor(warpDisplayName));
            var loreList = new ArrayList<String>();
            if (warp.description != null) {
                loreList.add(ChatUtils.hexColor(warp.description));
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
            inventory.addItem(item);
        }
    }

    private void openHomeMenu(HumanEntity playerEntity) {
        var layers = (int) Math.clamp(Math.ceil(getHomes(playerEntity).size() / 9f), 1, 6);
        var player = (Player) playerEntity;
        homesMenu.createAndOpenMenu(playerEntity, layers, langManager.getMessage("home.menu_title", player.getLocale(), new Placeholder("srcPlayer", player.getDisplayName())));
    }

    private void setHome(HumanEntity player, String name, Home home) {
        var section = homesConfig.createSection("players." + player.getUniqueId() + ".homes." + name);
        home.serialize(section);
        ConfigUtils.saveConfig(plugin, homesConfigFilePath, homesConfig);
    }

    private void removeHome(HumanEntity player, String name) {
        homesConfig.set("players." + player.getUniqueId() + ".homes." + name, null);
        ConfigUtils.saveConfig(plugin, homesConfigFilePath, homesConfig);
    }

    private boolean homeExists(HumanEntity player, String name) {
        return homesConfig.getConfigurationSection("players." + player.getUniqueId() + ".homes." + name) != null;
    }

    private Home getHome(HumanEntity player, String name) {
        var home = new Home();
        var section = homesConfig.getConfigurationSection("players." + player.getUniqueId() + ".homes." + name);
        if (section != null) {
            home.deserialize(section);
        }
        return home;
    }

    private List<String> getHomes(HumanEntity player) {
        var section = homesConfig.getConfigurationSection("players." + player.getUniqueId() + ".homes");
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(false));

    }

    public int getMaxAllowedHomes(final HumanEntity player) {
        final Pattern maxHomesPerm = Pattern.compile("sparkutils.home.limit\\.(\\d+)");

        return player.getEffectivePermissions().stream().map(i -> {
            Matcher matcher = maxHomesPerm.matcher(i.getPermission());
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1));
            }
            return 0;
        }).max(Integer::compareTo).orElse(0);
    }
}
