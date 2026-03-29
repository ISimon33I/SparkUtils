package com.isimon33i.sparkutils.modules.warp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

public class WarpModule extends Module {

    InventoryMenu warpMenu = new InventoryMenu();

    final String warpsConfigFilePath = "warps.yml";
    FileConfiguration warpsConfig;

    public final NamespacedKey warpNameKey;

    public WarpModule(Main plugin) {
        super(plugin);
        warpNameKey = new NamespacedKey(plugin, "warp.tp");
    }

    @Override
    public void onRegister() {
        registerCommand(plugin, "warp", this);
        registerCommand(plugin, "setwarp", this);
        registerCommand(plugin, "editwarp", this);

        warpsConfig = ConfigUtils.createConfig(plugin, warpsConfigFilePath, true, false);

        warpMenu.onMenuClick = x -> onMenuClick(x);
        warpMenu.onPopulateMenu = (x, y) -> populateMeny(x, y);
        warpMenu.initialize(plugin);
    }

    @Override
    public void onUnregister() {
        warpMenu.closeAllInventories(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
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
        args = ChatUtils.parseQuotedArgs(args).toArray(String[]::new);
        final var playerPlaceholder = new Placeholder("srcPlayer", srcDisplayName);
        if (command.getName().equalsIgnoreCase("warp")) {
            if (player != null) {
                if (args.length == 0) {
                    openWarpMenu(player);
                    return true;
                } else {
                    var warpName = args[0];
                    var warpNamePlaceholder = new Placeholder("warp", warpName);
                    var warp = getWarp(warpName);
                    var hasPermission = hasWarpPermission(warpName, player);
                    if (warp != null && hasPermission) {
                        if (warp.teleportEntity(player)) {
                            player.sendMessage(langManager.getMessage("warp.teleporting", locale, playerPlaceholder, warpNamePlaceholder));
                            return true;
                        }
                    } else {
                        player.sendMessage(langManager.getMessage("warp.not_exists", locale, playerPlaceholder, warpNamePlaceholder));
                        return true;
                    }
                }
            } else {
                sender.sendMessage(langManager.getMessage("warp.only_players_can_use", locale));
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("setwarp")) {
            if (player != null) {
                if (args.length > 0) {
                    var warpName = args[0];
                    if (warpExists(warpName)) {
                        player.sendMessage(langManager.getMessage("warp.already_exists", locale, playerPlaceholder));
                        return true;
                    } else {
                        var warp = new Warp();
                        if (args.length > 1) {
                            warp.restricted = Boolean.parseBoolean(args[1]);
                        }
                        warp.location = player.getLocation();
                        setWarp(args[0], warp);
                        player.sendMessage(langManager.getMessage("warp.created", locale, playerPlaceholder));
                        return true;
                    }
                }
            } else {
                sender.sendMessage(langManager.getMessage("warp.only_players_can_use", locale));
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("editwarp") && args.length > 1) {
            var warpName = args[0];
            var warp = getWarp(warpName);
            if (args[1].equalsIgnoreCase("remove")) {
                removeWarp(warpName);
                sender.sendMessage(langManager.getMessage("warp.removed", locale));
                return true;
            } else if (args[1].equalsIgnoreCase("move")) {
                if (player != null) {
                    warp.location = player.getLocation();
                    setWarp(warpName, warp);
                    sender.sendMessage(langManager.getMessage("warp.moved", locale));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("rename")) {
                if (args.length == 3) {
                    var newName = args[2];
                    if (warpExists(newName)) {
                        sender.sendMessage(langManager.getMessage("warp.warp_with_name_already_exists", locale));
                        return true;
                    }
                    removeWarp(warpName);
                    setWarp(newName, warp);
                    sender.sendMessage(langManager.getMessage("warp.renamed", locale));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("icon")) {
                if (args.length > 2) {
                    var iconName = args[2];
                    warp.icon = Material.matchMaterial(iconName);
                    setWarp(warpName, warp);
                    sender.sendMessage(langManager.getMessage("warp.icon_changed", locale));
                    return true;
                } else {
                    sender.sendMessage(langManager.getMessage("warp.current_icon", locale, new Placeholder("icon", warp.icon != null ? warp.icon.name() : langManager.getMessage("warp.no_icon", locale))));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("displayname")) {
                if (args.length > 2) {
                    warp.displayName = args[2];
                    setWarp(warpName, warp);
                    sender.sendMessage(langManager.getMessage("warp.displayname_changed", locale));
                    return true;
                } else {
                    sender.sendMessage(langManager.getMessage("warp.current_displayname", locale, new Placeholder("displayname", warp.displayName != null ? warp.displayName : langManager.getMessage("warp.no_displayname", locale))));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("description")) {
                if (args.length > 2) {
                    warp.description = args[2];
                    setWarp(warpName, warp);
                    sender.sendMessage(langManager.getMessage("warp.description_changed", locale));
                    return true;
                } else {
                    sender.sendMessage(langManager.getMessage("warp.current_description", locale, new Placeholder("description", warp.description != null ? warp.description : langManager.getMessage("warp.no_description", locale))));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("restricted")) {
                if (args.length > 2) {
                    warp.restricted = Boolean.parseBoolean(args[2]);
                    setWarp(warpName, warp);
                    sender.sendMessage(langManager.getMessage("warp.restriction_changed", locale));
                    return true;
                } else {
                    sender.sendMessage(langManager.getMessage("warp.current_restriction", locale, new Placeholder("restriction", warp.restricted ? langManager.getMessage("warp.yes", locale) : langManager.getMessage("warp.no", locale))));
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String string, String[] args) {
        if (command.getName().equalsIgnoreCase("warp") && args.length == 1) {
            if (sender instanceof Player player) {
                return filterByStart(args[args.length - 1], new ArrayList<>(getWarps(player)), false);
            }
            return new ArrayList<>();
        } else if (command.getName().equalsIgnoreCase("setwarp")) {
            if (args.length == 2) {
                return filterByStart(args[args.length - 1], List.of("true", "false"), false);
            }
        }
        if (command.getName().equalsIgnoreCase("editwarp")) {
            if (args.length == 1) {
                return filterByStart(args[args.length - 1], new ArrayList<>(getWarps()), false);
            }
            if (args.length == 2) {
                return filterByStart(args[args.length - 1], List.of("remove", "move", "rename", "icon", "displayname", "description", "restricted"), false);
            } else if (args[1].equalsIgnoreCase("icon")) {
                if (args.length == 3) {
                    return filterByStart(args[args.length - 1], Arrays.stream(Material.values()).map(Object::toString).toList(), false);
                }
            } else if (args[1].equalsIgnoreCase("restricted")) {
                if (args.length == 3) {
                    return filterByStart(args[args.length - 1], List.of("true", "false"), false);
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
                var warpName = data.get(warpNameKey, PersistentDataType.STRING);
                if (warpName != null) {
                    var player = event.getWhoClicked();
                    if (getWarp(warpName).teleportEntity(player)) {
                        player.sendMessage(langManager.getMessage("warp.teleporting", ((Player) player).getLocale()));
                    }
                }
            }
        }
    }

    private void populateMeny(HumanEntity player, Inventory inventory) {
        for (var warpName : getWarps(player)) {
            var warp = getWarp(warpName);

            var warpDisplayName = (warp.displayName == null || warp.displayName.isEmpty()) ? warpName : warp.displayName;
            var itemMaterial = warp.icon != null ? warp.icon : Material.GRASS_BLOCK;

            var item = new ItemStack(itemMaterial);
            var meta = item.getItemMeta();
            if (meta == null) {
                continue;
            }
            meta.getPersistentDataContainer().set(warpNameKey, PersistentDataType.STRING, warpName);

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

    private void openWarpMenu(HumanEntity playerEntity) {
        int layers = (int) Math.clamp(Math.ceil(getWarps(playerEntity).size() / 9f), 1, 6);
        var player = ((Player) playerEntity);
        warpMenu.createAndOpenMenu(playerEntity, layers, langManager.getMessage("warp.menu_title", player.getLocale(), new Placeholder("srcPlayer", player.getDisplayName())));
    }

    private void setWarp(String name, Warp warp) {
        var section = warpsConfig.createSection("warps." + name);
        warp.serialize(section);
        ConfigUtils.saveConfig(plugin, warpsConfigFilePath, warpsConfig);
    }

    private void removeWarp(String name) {
        warpsConfig.set("warps." + name, null);
        ConfigUtils.saveConfig(plugin, warpsConfigFilePath, warpsConfig);
    }

    private boolean warpExists(String name) {
        return warpsConfig.getConfigurationSection("warps." + name) != null;
    }

    private Warp getWarp(String name) {
        var warp = new Warp();
        var section = warpsConfig.getConfigurationSection("warps." + name);
        if (section != null) {
            warp.deserialize(section);
        }
        return warp;
    }

    private List<String> getWarps() {
        return getWarps(null);
    }

    private List<String> getWarps(HumanEntity player) {
        var section = warpsConfig.getConfigurationSection("warps");
        if (section == null) {
            return new ArrayList<>();
        }
        if (player == null) {
            return new ArrayList<>(section.getKeys(false));
        } else {
            return section.getKeys(false).stream().filter(x -> hasWarpPermission(x, player)).toList();
        }
    }

    private boolean hasWarpPermission(String name, HumanEntity player) {
        var warp = getWarp(name);
        if (warp == null) {
            return false;
        }
        return !warp.restricted || player.hasPermission("sparkutils.warp.warps." + name);
    }
}
