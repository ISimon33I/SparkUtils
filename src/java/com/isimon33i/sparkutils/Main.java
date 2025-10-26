package com.isimon33i.sparkutils;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import com.isimon33i.sparkutils.modules.AntiGriefing;
import com.isimon33i.sparkutils.modules.Spawn;
import com.isimon33i.sparkutils.modules.TPA;
import com.isimon33i.sparkutils.modules.Time;
import com.isimon33i.sparkutils.modules.home.HomeModule;
import com.isimon33i.sparkutils.modules.warp.WarpModule;
import com.isimon33i.utils.lang.LanguageManager;

public class Main extends JavaPlugin {

    private static Main instance;

    public static Main GetInstance() {
        return instance;
    }

    TPA tpa;
    WarpModule warp;
    HomeModule home;
    Spawn spawn;
    AntiGriefing antiGriefing;
    Time time;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        
        saveDefaultConfig();
        
        saveResource("lang/", false);
        saveResource("lang/en_us.txt", false);
        saveResource("lang/sv_se.txt", false);
        LanguageManager.Instance.debug = getConfig().getBoolean("lang_debug");
        getServer().getConsoleSender().sendMessage("LangDebug: " + LanguageManager.Instance.debug);
        LanguageManager.Instance.initialize(new File(getDataFolder(), "lang"));

        tpa = new TPA(this);
        warp = new WarpModule(this);
        home = new HomeModule(this);
        spawn = new Spawn(this);
        antiGriefing = new AntiGriefing(this);
        time = new Time(this);

        tpa.onRegister(this);
        warp.onRegister(this);
        home.onRegister(this);
        spawn.onRegister(this);
        antiGriefing.onRegister(this);
        time.onRegister(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        
        spawn.onUnregister(this);
        home.onUnregister(this);
        warp.onUnregister(this);
        tpa.onUnregister(this);

        getServer().getScheduler().cancelTasks(this);
    }
}
