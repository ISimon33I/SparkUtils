package com.isimon33i.sparkutils.modules;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.isimon33i.sparkutils.Main;
import com.isimon33i.utils.ConfigUtils;
import com.isimon33i.utils.SunCalc;

public class Time extends Module implements Runnable {
    
    final double realToGameScaleFactor = 24000D/(60*60*24);
    
    final String timeConfigFilePath = "time.yml";
    FileConfiguration timeConfig;
    
    final String configTimeModeKey = "time-mode";
	final String configTimeSpeedKey = "time-speed";
	final String configRealTimeDayLengthCompensationKey = "real-time-day-length-compensation";
    final String configTimezoneKey = "timezone";
    final String configLatitudeKey = "latitude";
    final String configLongitudeKey = "longitude";
    
    HashMap<UUID, Double> timeCounterMap;
    
    public Time(Main plugin) {
        super(plugin);
        timeCounterMap = new HashMap<>();
    }

    @Override
    public void onRegister() {
        timeConfig = ConfigUtils.createConfig(plugin, timeConfigFilePath, true, false);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 1);
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender cs, Command cmnd, String string, String[] strings) {
        return null;
    }

    @Override
    public void run() {
        var worldsConfigSection = timeConfig.getConfigurationSection("worlds");
        if(worldsConfigSection != null) {
            for (var worldConfigKey : worldsConfigSection.getKeys(false)) {
                var worldConfigSection = worldsConfigSection.getConfigurationSection(worldConfigKey);
                if (worldConfigSection != null) {
                    var world = Bukkit.getWorld(worldConfigKey);
                    if (world != null) {
                        var dayLightCycle = world.getGameRuleValue(GameRule.ADVANCE_TIME);
                        
                        switch(worldConfigSection.getInt(configTimeModeKey, 0)) {
                            case 0 -> {
                                var speed = worldConfigSection.getDouble(configTimeSpeedKey, 1.0);
                                if (speed != 1.0) {
                                    if(dayLightCycle==true) world.setGameRule(GameRule.ADVANCE_TIME, false);
                                    
                                    var timeCounter = timeCounterMap.getOrDefault(world.getUID(), 0D);
                                    timeCounter += speed;
                                    var ticks = (long)Math.floor(Math.abs(timeCounter));
                                    
                                    var fullTime = world.getFullTime();
                                    if(timeCounter>=0){
                                        timeCounter -= ticks;
                                        world.setFullTime(fullTime+ticks);
                                    }else{
                                        timeCounter += ticks;
                                        world.setFullTime(fullTime-ticks);
                                    }
                                    
                                    timeCounterMap.put(world.getUID(), timeCounter);
                                }
                            }
                            case 1 -> {
                                if(dayLightCycle==true) world.setGameRule(GameRule.ADVANCE_TIME, false);
                                var dayLengthCompensation = worldConfigSection.getBoolean(configRealTimeDayLengthCompensationKey, true);
                                final long minecraftTime;
                                if(dayLengthCompensation){
                                    var timezone = timeConfig.getString(configTimezoneKey);
                                    var latitude = timeConfig.getDouble(configLatitudeKey);
                                    var longitude = timeConfig.getDouble(configLongitudeKey);
                                    
                                    var sunInfo = SunCalc.calculateSunriseSunset(LocalDate.now(), latitude, longitude, ZoneId.of(timezone));
                                    minecraftTime = SunCalc.mapToMinecraftTime(LocalTime.now(), sunInfo.sunrise, sunInfo.sunset);
                                }else{
                                    long sec = LocalTime.now().toNanoOfDay() / 1_000_000_000;
                                    long gameTicks = (long)(sec * realToGameScaleFactor);
                                    long offsetedGameTicks = gameTicks - 6000;
                                    long ticks = offsetedGameTicks;
                                    if(ticks < 0) {
                                        ticks += 24000;
                                    }
                                    
                                    minecraftTime = ticks;
                                }
                                
                                var fullTime = world.getFullTime();
                                fullTime = (long)(Math.floor(fullTime/24000D) * 24000);
                                world.setFullTime(fullTime + minecraftTime);
                            }
                        }
                        
                    }
                }
            }
        }
    }
    
}