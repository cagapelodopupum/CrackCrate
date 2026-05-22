package com.crackcrate.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import java.util.List;
import java.util.Map;

public class CrateReward {
    private final String id;
    private final String displayName;
    private final Material material;
    private final int amount;
    private final double chance;
    private final List<String> commands;
    private final Map<Enchantment, Integer> enchants;

    public CrateReward(String id, String displayName, Material material, int amount,
                       double chance, List<String> commands, Map<Enchantment, Integer> enchants) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.amount = amount;
        this.chance = chance;
        this.commands = commands;
        this.enchants = enchants;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public double getChance() { return chance; }
    public List<String> getCommands() { return commands; }
    public Map<Enchantment, Integer> getEnchants() { return enchants; }
}
