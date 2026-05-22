package com.crackcrate.managers;

import com.crackcrate.CrackCrate;
import com.crackcrate.models.CrateReward;
import com.crackcrate.models.CrateType;
import com.crackcrate.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class CrateManager {

    private final CrackCrate plugin;
    private final Map<String, CrateType> crateTypes = new LinkedHashMap<>();
    private final Set<UUID> openingPlayers = new HashSet<>();

    public CrateManager(CrackCrate plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    public void loadCrates() {
        crateTypes.clear();
        ConfigurationSection cratesSection = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSection == null) return;

        for (String crateId : cratesSection.getKeys(false)) {
            ConfigurationSection cs = cratesSection.getConfigurationSection(crateId);
            if (cs == null) continue;

            String displayName = ColorUtils.colorize(cs.getString("display-name", crateId));
            String keyName = ColorUtils.colorize(cs.getString("key-name", crateId + " Key"));
            List<String> keyLore = ColorUtils.colorizeList(cs.getStringList("key-lore"));
            Material keyMat = parseMaterial(cs.getString("key-material", "TRIPWIRE_HOOK"));
            Material blockMat = parseMaterial(cs.getString("block-material", "CHEST"));
            List<String> hologramLines = ColorUtils.colorizeList(cs.getStringList("hologram-lines"));

            List<CrateReward> rewards = new ArrayList<>();
            ConfigurationSection rewardsSection = cs.getConfigurationSection("rewards");
            if (rewardsSection != null) {
                for (String rewardId : rewardsSection.getKeys(false)) {
                    ConfigurationSection rs = rewardsSection.getConfigurationSection(rewardId);
                    if (rs == null) continue;

                    String rewardName = ColorUtils.colorize(rs.getString("display-name", rewardId));
                    Material mat = parseMaterial(rs.getString("material", "STONE"));
                    int amount = rs.getInt("amount", 1);
                    double chance = rs.getDouble("chance", 10.0);
                    List<String> commands = rs.getStringList("commands");

                    Map<Enchantment, Integer> enchants = new HashMap<>();
                    ConfigurationSection enchSection = rs.getConfigurationSection("enchants");
                    if (enchSection != null) {
                        for (String enchKey : enchSection.getKeys(false)) {
                            Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchKey.toLowerCase()));
                            if (ench != null) {
                                enchants.put(ench, enchSection.getInt(enchKey));
                            }
                        }
                    }

                    rewards.add(new CrateReward(rewardId, rewardName, mat, amount, chance, commands, enchants));
                }
            }

            CrateType crate = new CrateType(crateId, displayName, keyName, keyLore,
                    keyMat, blockMat, hologramLines, rewards);

            // Load saved location
            ConfigurationSection locSection = plugin.getLocationsConfig().getConfigurationSection("locations." + crateId);
            if (locSection != null) {
                try {
                    World world = Bukkit.getWorld(locSection.getString("world", "world"));
                    double x = locSection.getDouble("x");
                    double y = locSection.getDouble("y");
                    double z = locSection.getDouble("z");
                    if (world != null) {
                        crate.setLocation(new Location(world, x, y, z));
                    }
                } catch (Exception ignored) {}
            }

            crateTypes.put(crateId, crate);
        }

        plugin.getLogger().info("Carregadas " + crateTypes.size() + " crates!");
    }

    public void openCrate(Player player, CrateType crate) {
        if (openingPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "") +
                    "&cVocê já está abrindo uma crate!"));
            return;
        }

        if (!hasKey(player, crate)) {
            String msg = plugin.getConfig().getString("messages.no-key", "&cSem chave!");
            player.sendMessage(ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "") + msg));
            return;
        }

        removeKey(player, crate);
        openingPlayers.add(player.getUniqueId());

        // Play open sound
        if (plugin.getConfig().getBoolean("settings.sound-enabled", true)) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        }

        // Particles
        if (plugin.getConfig().getBoolean("settings.particles-enabled", true)) {
            spawnOpenParticles(player.getLocation());
        }

        int duration = plugin.getConfig().getInt("settings.animation-duration", 3);

        // Animation then reward
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            CrateReward reward = rollReward(crate);
            if (reward == null) {
                openingPlayers.remove(player.getUniqueId());
                return;
            }

            giveReward(player, reward);
            openingPlayers.remove(player.getUniqueId());

            String msg = plugin.getConfig().getString("messages.crate-opened", "")
                    .replace("%crate%", crate.getDisplayName())
                    .replace("%reward%", reward.getDisplayName());
            player.sendMessage(ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "") + msg));

            if (plugin.getConfig().getBoolean("settings.sound-enabled", true)) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }

            // Broadcast if rare
            double rareThreshold = plugin.getConfig().getDouble("settings.rare-threshold", 5.0);
            boolean broadcastRare = plugin.getConfig().getBoolean("settings.broadcast-rare", true);
            if (broadcastRare && reward.getChance() <= rareThreshold) {
                String broadcast = plugin.getConfig().getString("messages.broadcast-rare", "")
                        .replace("%player%", player.getName())
                        .replace("%crate%", crate.getDisplayName())
                        .replace("%reward%", reward.getDisplayName());
                String formatted = String.format(
                        ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "") + broadcast),
                        reward.getChance()
                );
                Bukkit.broadcastMessage(formatted);
            }

        }, duration * 20L);
    }

    public CrateReward rollReward(CrateType crate) {
        List<CrateReward> rewards = crate.getRewards();
        if (rewards.isEmpty()) return null;

        double totalChance = crate.getTotalChance();
        double roll = ThreadLocalRandom.current().nextDouble(0, totalChance);

        double cumulative = 0;
        for (CrateReward reward : rewards) {
            cumulative += reward.getChance();
            if (roll < cumulative) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    private void giveReward(Player player, CrateReward reward) {
        // Give item
        if (reward.getMaterial() != Material.AIR) {
            ItemStack item = new ItemStack(reward.getMaterial(), reward.getAmount());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(reward.getDisplayName());
                item.setItemMeta(meta);
            }
            // Apply enchants
            if (!reward.getEnchants().isEmpty()) {
                reward.getEnchants().forEach((ench, lvl) -> item.addUnsafeEnchantment(ench, lvl));
            }
            player.getInventory().addItem(item);
        }

        // Execute commands
        for (String cmd : reward.getCommands()) {
            String finalCmd = cmd.replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        }
    }

    public ItemStack createKey(CrateType crate, int amount) {
        ItemStack key = new ItemStack(crate.getKeyMaterial(), amount);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(crate.getKeyName());
            List<String> lore = new ArrayList<>(crate.getKeyLore());
            meta.setLore(lore);
            // Tag the key with crate id via persistent data
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "crate_key"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    crate.getId()
            );
            key.setItemMeta(meta);
        }
        return key;
    }

    public boolean hasKey(Player player, CrateType crate) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isKey(item, crate)) return true;
        }
        return false;
    }

    public void removeKey(Player player, CrateType crate) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isKey(contents[i], crate)) {
                if (contents[i].getAmount() > 1) {
                    contents[i].setAmount(contents[i].getAmount() - 1);
                } else {
                    player.getInventory().setItem(i, null);
                }
                return;
            }
        }
    }

    public boolean isKey(ItemStack item, CrateType crate) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(plugin, "crate_key");
        if (!meta.getPersistentDataContainer().has(key)) return false;
        String keyId = meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        return crate.getId().equals(keyId);
    }

    public String getCrateIdFromKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(plugin, "crate_key");
        if (!meta.getPersistentDataContainer().has(key)) return null;
        return meta.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
    }

    private void spawnOpenParticles(Location loc) {
        Location center = loc.clone().add(0.5, 1, 0.5);
        loc.getWorld().spawnParticle(Particle.FIREWORK, center, 30, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 15, 0.5, 0.5, 0.5, 0);
    }

    public void saveLocation(CrateType crate, Location loc) {
        crate.setLocation(loc);
        String path = "locations." + crate.getId();
        plugin.getLocationsConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getLocationsConfig().set(path + ".x", loc.getBlockX());
        plugin.getLocationsConfig().set(path + ".y", loc.getBlockY());
        plugin.getLocationsConfig().set(path + ".z", loc.getBlockZ());
        plugin.saveLocationsConfig();
    }

    public CrateType getCrateAtLocation(Location loc) {
        for (CrateType crate : crateTypes.values()) {
            if (crate.getLocation() == null) continue;
            Location cl = crate.getLocation();
            if (cl.getBlockX() == loc.getBlockX()
                    && cl.getBlockY() == loc.getBlockY()
                    && cl.getBlockZ() == loc.getBlockZ()
                    && cl.getWorld().equals(loc.getWorld())) {
                return crate;
            }
        }
        return null;
    }

    public boolean isOpeningCrate(UUID uuid) {
        return openingPlayers.contains(uuid);
    }

    public Map<String, CrateType> getCrateTypes() { return crateTypes; }

    public CrateType getCrate(String id) { return crateTypes.get(id.toLowerCase()); }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return Material.CHEST;
        }
    }
}
