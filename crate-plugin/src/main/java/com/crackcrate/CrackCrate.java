package com.crackcrate;

import com.crackcrate.commands.CrateCommand;
import com.crackcrate.listeners.CrateListener;
import com.crackcrate.managers.CrateManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class CrackCrate extends JavaPlugin {

    private CrateManager crateManager;
    private File locationsFile;
    private FileConfiguration locationsConfig;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Load locations file
        locationsFile = new File(getDataFolder(), "locations.yml");
        if (!locationsFile.exists()) {
            try { locationsFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);

        // Init manager
        crateManager = new CrateManager(this);

        // Register commands
        CrateCommand crateCmd = new CrateCommand(this);
        getCommand("crate").setExecutor(crateCmd);
        getCommand("crate").setTabCompleter(crateCmd);
        getCommand("givecrate").setExecutor(crateCmd);
        getCommand("givecrate").setTabCompleter(crateCmd);
        getCommand("keyall").setExecutor(crateCmd);
        getCommand("keyall").setTabCompleter(crateCmd);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);

        getLogger().info("╔═══════════════════════════╗");
        getLogger().info("║   CrackCrate v1.0.0       ║");
        getLogger().info("║   Plugin de Crates Ativo! ║");
        getLogger().info("╚═══════════════════════════╝");
    }

    @Override
    public void onDisable() {
        getLogger().info("CrackCrate desativado.");
    }

    public CrateManager getCrateManager() { return crateManager; }

    public FileConfiguration getLocationsConfig() { return locationsConfig; }

    public void saveLocationsConfig() {
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            getLogger().severe("Erro ao salvar locations.yml: " + e.getMessage());
        }
    }
}
