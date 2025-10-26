package com.isimon33i.utils;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryMenu implements Listener {
    
    HashMap<UUID, Inventory> inventories = new HashMap<>();
    
    public Consumer<InventoryClickEvent> onMenuClick;
    public BiConsumer<HumanEntity, Inventory> onPopulateMenu;
    
    public void initialize(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    public void closeAllInventories(JavaPlugin plugin){
        inventories.forEach((uuid, inventory) -> {
            var player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.getOpenInventory().getTopInventory() == inventory) {
                player.closeInventory();
            }
        });
    }
    
    @EventHandler
    @SuppressWarnings("unused")
    private void onInventoryClick(InventoryClickEvent event) {
        var player = event.getWhoClicked();
        if (event.getInventory() == inventories.get(player.getUniqueId())) {
            event.setCancelled(true);
            if(onMenuClick != null) onMenuClick.accept(event);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    private void onInventoryClose(InventoryCloseEvent event) {
        var player = event.getPlayer().getUniqueId();
        if (event.getInventory() == inventories.get(player)) {
            inventories.remove(player);
        }
    }

    public void createAndOpenMenu(HumanEntity player, int rows, String title) {
        Inventory inv;
        if (!inventories.containsKey(player.getUniqueId())) {
            inv = Bukkit.createInventory(null, 9 * rows, title);
            inventories.put(player.getUniqueId(), inv);

        } else {
            inv = inventories.get(player.getUniqueId());
        }
        
        inv.clear();
        if(onPopulateMenu != null) onPopulateMenu.accept(player, inv);
        
        player.openInventory(inv);
    }
}