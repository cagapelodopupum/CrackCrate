package com.crackcrate.models;

import org.bukkit.Location;
import org.bukkit.Material;
import java.util.List;

public class CrateType {
    private final String id;
    private final String displayName;
    private final String keyName;
    private final List<String> keyLore;
    private final Material keyMaterial;
    private final Material blockMaterial;
    private final List<String> hologramLines;
    private final List<CrateReward> rewards;
    private Location location;

    public CrateType(String id, String displayName, String keyName, List<String> keyLore,
                     Material keyMaterial, Material blockMaterial, List<String> hologramLines,
                     List<CrateReward> rewards) {
        this.id = id;
        this.displayName = displayName;
        this.keyName = keyName;
        this.keyLore = keyLore;
        this.keyMaterial = keyMaterial;
        this.blockMaterial = blockMaterial;
        this.hologramLines = hologramLines;
        this.rewards = rewards;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getKeyName() { return keyName; }
    public List<String> getKeyLore() { return keyLore; }
    public Material getKeyMaterial() { return keyMaterial; }
    public Material getBlockMaterial() { return blockMaterial; }
    public List<String> getHologramLines() { return hologramLines; }
    public List<CrateReward> getRewards() { return rewards; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public double getTotalChance() {
        return rewards.stream().mapToDouble(CrateReward::getChance).sum();
    }
}
