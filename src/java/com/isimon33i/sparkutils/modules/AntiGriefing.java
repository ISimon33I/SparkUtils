package com.isimon33i.sparkutils.modules;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ConfigUtils;
import static com.isimon33i.utils.Utils.filterByStart;
import com.isimon33i.utils.lang.Placeholder;

public class AntiGriefing extends Module implements Listener {
    
    FileConfiguration antiGrifingConfig;
    final String antiGrifingConfigFilePath = "anti_griefing.yml";
	
	final String configPreventTntBlockDamageKey = "prevent-tnt-block-damage";
	final String configPreventTntEntityDamageKey = "prevent-tnt-entity-damage";
	final String configPreventCreeperBlockDamageKey = "prevent-creeper-block-damage";
	final String configPreventCreeperEntityDamageKey = "prevent-creeper-entity-damage";
	final String configPreventZombieBreakDoorKey = "prevent-zombie-break-door";
    
    public AntiGriefing(Main plugin) {
        super(plugin);
    }

    @Override
    public void onRegister() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        antiGrifingConfig = ConfigUtils.createConfig(plugin, antiGrifingConfigFilePath, true, false);
		
		registerCommand(plugin, "antigrief", this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String string, String[] args) {
		if(sender == null) return false;
		Player player;
        String locale = "en_US";
        if (sender instanceof Player p) {
            player = p;
            locale = player.getLocale();
        }
		
		if(command.getName().equalsIgnoreCase("antigrief")){
			if(args.length == 3){
				if (args[0].equalsIgnoreCase("set")) {
					var state = Boolean.parseBoolean(args[2]);
					if (args[1].equalsIgnoreCase("tntblockdamage")) {
						antiGrifingConfig.set(configPreventTntBlockDamageKey, !state);
						ConfigUtils.saveConfig(plugin, antiGrifingConfigFilePath, antiGrifingConfig);
						return true;
					} else if (args[1].equalsIgnoreCase("tntentitydamage")) {
						antiGrifingConfig.set(configPreventTntEntityDamageKey, !state);
						ConfigUtils.saveConfig(plugin, antiGrifingConfigFilePath, antiGrifingConfig);
						return true;
					} else if (args[1].equalsIgnoreCase("creeperblockdamage")) {
						antiGrifingConfig.set(configPreventCreeperBlockDamageKey, !state);
						ConfigUtils.saveConfig(plugin, antiGrifingConfigFilePath, antiGrifingConfig);
						return true;
					} else if (args[1].equalsIgnoreCase("creeperentitydamage")) {
						antiGrifingConfig.set(configPreventCreeperEntityDamageKey, !state);
						ConfigUtils.saveConfig(plugin, antiGrifingConfigFilePath, antiGrifingConfig);
						return true;
					} else if (args[1].equalsIgnoreCase("zombiebreakdoor")) {
						antiGrifingConfig.set(configPreventZombieBreakDoorKey, !state);
						ConfigUtils.saveConfig(plugin, antiGrifingConfigFilePath, antiGrifingConfig);
						return true;
					}
				}
			} else if (args.length == 2) {
				if(args[0].equalsIgnoreCase("get")){
					if (args[1].equalsIgnoreCase("tntblockdamage")) {
						var state = antiGrifingConfig.getBoolean(configPreventTntBlockDamageKey, false);
						sender.sendMessage(langManager.getMessage("antigrief.tnt_block_damage_enabled", locale, new Placeholder("state", state?langManager.getMessage("antigrief.disabled", locale):langManager.getMessage("antigrief.enabled", locale))));
						return true;
					} else if (args[1].equalsIgnoreCase("tntentitydamage")) {
						var state = antiGrifingConfig.getBoolean(configPreventTntEntityDamageKey, false);
						sender.sendMessage(langManager.getMessage("antigrief.tnt_entity_damage_enabled", locale, new Placeholder("state", state?langManager.getMessage("antigrief.disabled", locale):langManager.getMessage("antigrief.enabled", locale))));
						return true;
					} else if (args[1].equalsIgnoreCase("creeperblockdamage")) {
						var state = antiGrifingConfig.getBoolean(configPreventCreeperBlockDamageKey, false);
						sender.sendMessage(langManager.getMessage("antigrief.creeper_block_damage_enabled", locale, new Placeholder("state", state?langManager.getMessage("antigrief.disabled", locale):langManager.getMessage("antigrief.enabled", locale))));
						return true;
					} else if (args[1].equalsIgnoreCase("creeperentitydamage")) {
						var state = antiGrifingConfig.getBoolean(configPreventCreeperEntityDamageKey, false);
						sender.sendMessage(langManager.getMessage("antigrief.creeper_entity_damage_enabled", locale, new Placeholder("state", state?langManager.getMessage("antigrief.disabled", locale):langManager.getMessage("antigrief.enabled", locale))));
						return true;
					} else if (args[1].equalsIgnoreCase("zombiebreakdoor")) {
						var state = antiGrifingConfig.getBoolean(configPreventZombieBreakDoorKey, false);
						sender.sendMessage(langManager.getMessage("antigrief.zombie_break_door_enabled", locale, new Placeholder("state", state?langManager.getMessage("antigrief.disabled", locale):langManager.getMessage("antigrief.enabled", locale))));
						return true;
					}
				}
			}
		}
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String string, String[] args) {
		if (command.getName().equalsIgnoreCase("antigrief")) {
			if(args.length == 1) {
				return filterByStart(args[0], List.of("set", "get"), false);
			} else if(args.length == 2) {
				return filterByStart(args[1], List.of("tntblockdamage", "tntentitydamage", "creeperblockdamage", "creeperentitydamage", "zombiebreakdoor"), false);
			} else if (args.length == 3 && args[2].equalsIgnoreCase("set")) {
				return filterByStart(args[2], List.of("true", "false"), false);
			}
		}
        return null;
    }
    
    @EventHandler
	public void onExplosion(EntityExplodeEvent e) {
		EntityType type = e.getEntity().getType();
		if(type == EntityType.TNT || type == EntityType.TNT_MINECART){
			if(antiGrifingConfig.getBoolean(configPreventTntBlockDamageKey, false)){
				e.blockList().clear();
			}
		} else if (type == EntityType.CREEPER) {
			if(antiGrifingConfig.getBoolean(configPreventCreeperBlockDamageKey, false)){
				e.blockList().clear();
			}
		}
	}
	
	@EventHandler
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent e){
		DamageCause cause = e.getCause();
		if(cause == DamageCause.BLOCK_EXPLOSION){
			if(antiGrifingConfig.getBoolean(configPreventTntEntityDamageKey, false)){
				e.setDamage(0);
				e.setCancelled(true);
			}
		}
		if(cause == DamageCause.ENTITY_EXPLOSION){
			if(antiGrifingConfig.getBoolean(configPreventCreeperEntityDamageKey, false)){
				e.setDamage(0);
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onZombieBreakDoor(EntityBreakDoorEvent e){
		if(antiGrifingConfig.getBoolean(configPreventZombieBreakDoorKey)){
			if(e.getEntityType() == EntityType.ZOMBIE || e.getEntityType() == EntityType.HUSK || e.getEntityType() == EntityType.ZOMBIE_VILLAGER) {
				e.setCancelled(true);
			}
		}
	}
}