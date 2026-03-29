package com.isimon33i.sparkutils;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.isimon33i.sparkutils.modules.AntiGriefing;
import com.isimon33i.sparkutils.modules.Spawn;
import com.isimon33i.sparkutils.modules.TPA;
import com.isimon33i.sparkutils.modules.Time;
import com.isimon33i.sparkutils.modules.economy.EconomyModule;
import com.isimon33i.sparkutils.modules.home.HomeModule;
import com.isimon33i.sparkutils.modules.utilities.UtilitiesModule;
import com.isimon33i.sparkutils.modules.warp.WarpModule;
import com.isimon33i.utils.lang.LanguageManager;

public class Main extends JavaPlugin implements PluginMessageListener {

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
    EconomyModule economy;
    UtilitiesModule utilities;

    public TPA getTpa() {
        return tpa;
    }

    public WarpModule getWarp() {
        return warp;
    }

    public HomeModule getHome() {
        return home;
    }

    public Spawn getSpawn() {
        return spawn;
    }

    public AntiGriefing getAntiGriefing() {
        return antiGriefing;
    }

    public Time getTime() {
        return time;
    }

    public EconomyModule getEconomy() {
        return economy;
    }

    public UtilitiesModule getUtilities() {
        return utilities;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

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
        economy = new EconomyModule(this);
        utilities = new UtilitiesModule(this);

        tpa.onRegister();
        warp.onRegister();
        home.onRegister();
        spawn.onRegister();
        antiGriefing.onRegister();
        time.onRegister();
        economy.onRegister();
        utilities.onRegister();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);

        utilities.onUnregister();
        economy.onUnregister();
        time.onUnregister();
        antiGriefing.onUnregister();
        spawn.onUnregister();
        home.onUnregister();
        warp.onUnregister();
        tpa.onUnregister();

        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        getLogger().log(Level.INFO, "{0} | {1} | {2}", new Object[]{channel, player.getDisplayName(), in.toString()});

    }
}
