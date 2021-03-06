package me.rektb.bettershulkerboxes;

import me.rektb.bettershulkerboxes.commands.MainCommand;
import me.rektb.bettershulkerboxes.events.*;
import me.rektb.bettershulkerboxes.utils.ConfigurationImport;
import me.rektb.bettershulkerboxes.utils.Metrics;
import me.rektb.bettershulkerboxes.utils.ShulkerManage;
import me.rektb.bettershulkerboxes.utils.UpdateChecker;
import me.rektb.bettershulkerboxes.utils.worldguard.WorldGuardSupport;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterShulkerBoxes extends JavaPlugin implements Listener {
    public static BetterShulkerBoxes instance;

    //instance given to ConfigurationImport
    public BetterShulkerBoxes cfginst;
    public MainCommand maincmd;

    public InteractEvent interactEvent;
    public InvClickEvent invClickEvent;
    public InvCloseEvent invCloseEvent;
    public DupePreventEvents dupePreventEvents;
    public PlyrJoinEvent plyrJoinEvent;

    public boolean updatefound = false;
    public ArrayList<String> changes = new ArrayList<>();
    public String lastver = "";
    public String resourceurl = "";
    public UpdateChecker updater = new UpdateChecker(this, 58837);
    public ConfigurationImport cfgi;
    public ShulkerManage shlkm;

    public WorldGuardSupport wgs = null;

    @Override
    public void onLoad() {
        boolean worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        boolean worldEditEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
        if (worldGuardEnabled) {
            Bukkit.getConsoleSender().sendMessage("[BSB] World Guard detected, enabling WorldGuard support...");
            if (!worldEditEnabled) {
                Bukkit.getConsoleSender().sendMessage("[BSB] Could not start WorldGuard support, WoldEdit is missing.");
            } else if (getVersion() < 13) {
                Bukkit.getConsoleSender().sendMessage("[BSB] Could not start WorldGuard support, spigot 1.13 or later is required.");
            } else {
                wgs = new WorldGuardSupport(() -> {
                    Bukkit.getConsoleSender().sendMessage("[BSB] World Guard support enabled.");
                }, () -> {
                    Bukkit.getConsoleSender().sendMessage("[BSB] SEVERE ERROR - WORLD GUARD FLAG CONFLICT");
                    wgs = null;
                });
                wgs.init();
            }
        }
    }

    public void onEnable() {
        instance = this;


        loadConfig();
        checkConfigValidity();
        shlkm = new ShulkerManage();
        interactEvent = new InteractEvent();
        invClickEvent = new InvClickEvent(this);
        invCloseEvent = new InvCloseEvent();
        dupePreventEvents = new DupePreventEvents();
        plyrJoinEvent = new PlyrJoinEvent();
        maincmd = new MainCommand();

        getCommand(maincmd.command).setExecutor(new MainCommand());
        getServer().getPluginManager().registerEvents(interactEvent, this);
        getServer().getPluginManager().registerEvents(invClickEvent, this);
        getServer().getPluginManager().registerEvents(invCloseEvent, this);
        getServer().getPluginManager().registerEvents(dupePreventEvents, this);
        getServer().getPluginManager().registerEvents(plyrJoinEvent, this);
        getServer().getPluginManager().registerEvents(plyrJoinEvent, this);

        if (cfgi.cfg_statistics) {
            Metrics metrics = new Metrics(this, 6076);
            metrics.addCustomChart(new Metrics.SimplePie("cooldown_enabled", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return String.valueOf(cfgi.cfg_enablecooldown);
                }
            }));
            metrics.addCustomChart(new Metrics.SimplePie("right_click_air_enabled", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return String.valueOf(cfgi.cfg_rclickair);
                }
            }));
            metrics.addCustomChart(new Metrics.SimplePie("right_click_inventory_enabled", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return String.valueOf(cfgi.cfg_rightclick_enabled);
                }
            }));
            metrics.addCustomChart(new Metrics.SimplePie("right_click_container_enabled", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return String.valueOf(cfgi.cfg_rightclick_chest_enabled);
                }
            }));


        } else {
            getServer().getConsoleSender().sendMessage("Statistics have been disabled, please consider enabling them to" +
                    "help plugin development.");
        }
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Better Shulkerboxes enabled - Plugin written by Rektb");
        Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
            public void run() {
                BetterShulkerBoxes.this.getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "BSB is checking for updates...");
                try {
                    if (BetterShulkerBoxes.this.updater.checkForUpdates()) {
                        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Update found! You're using " + getDescription().getVersion() + " New version: " + BetterShulkerBoxes.this.updater.getLatestVersion() + ", download at: " + BetterShulkerBoxes.this.updater.getResourceURL());
                        BetterShulkerBoxes.this.updatefound = true;
                        BetterShulkerBoxes.this.changes = updater.getChangelog();
                        BetterShulkerBoxes.this.lastver = BetterShulkerBoxes.this.updater.getLatestVersion();
                        BetterShulkerBoxes.this.resourceurl = BetterShulkerBoxes.this.updater.getResourceURL();
                    } else {
                        BetterShulkerBoxes.this.getLogger().info(ChatColor.GREEN + "Better Shulker Boxes is up to date!");
                    }
                } catch (Exception e) {
                    BetterShulkerBoxes.this.getServer().getConsoleSender().sendMessage(ChatColor.RED + "Error! BSB could not check for updates:");
                    e.printStackTrace();
                }
            }
        });
    }

    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "Better Shulkerboxes disabled");
    }

    public void checkConfigValidity() {
        cfginst = this;
        cfgi = new ConfigurationImport();
        if (!cfgi.checkConfigurationValidity().isEmpty()) {
            throwConfigurationErrror(cfgi.checkConfigurationValidity());
        }
    }

    public void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
    }


    public void throwConfigurationErrror(String error) {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "<ERROR> BetterShulkerBoxes found an invalid configuration file. Please fix the following issue(s):");
        for (String s : error.split("-")) {
            getServer().getConsoleSender().sendMessage("-> " + s);
        }
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "Disabling plugin");
        Bukkit.getPluginManager().disablePlugin(this);
    }

    public void newInstances() {
        this.maincmd.getNewInstances();
        this.dupePreventEvents.getNewInstances();
        this.interactEvent.getNewInstances();
        this.invCloseEvent.getNewInstances();
        this.shlkm.getNewInstances();
    }

    public static BetterShulkerBoxes getInstance() {
        return instance;
    }

    public int getVersion() {
        final String regex = "([0-9]{1,2}\\.[0-9]{1,2}(\\.[0-9]{1,2})?)"; //This should get the MC version from "git-Spigot-2ee05fe-d31f05f (MC: 1.15.1)"
        final Matcher m = Pattern.compile(regex).matcher(getServer().getVersion());
        final List<String> matches = new ArrayList<>();
        while (m.find()) {
            matches.add(m.group(0));
        }
        String version = "invalid";
        if (matches.size() > 0) {
            version = matches.get(0);
        }
        int v = 0;
        if (!version.equals("invalid")) {
            v = Integer.parseInt(version.split("\\.")[1]);
        }
        if (v < 12) { // Just a warn when used in versions under 1.12
            getServer().getConsoleSender().sendMessage(ChatColor.RED + String.format("Warning! BetterShulkerBoxes does" +
                    " NOT support %s officially, if you find any problems contact the developer. I am not responsible" +
                    " for players duping items, the server breaking entirely or anything else.", version));
            getServer().getConsoleSender().sendMessage(version.split("\\.")[1]);
        }
        return v;
    }

}
