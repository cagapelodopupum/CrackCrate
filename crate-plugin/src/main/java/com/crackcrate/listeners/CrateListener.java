package com.crackcrate.listeners;

import com.crackcrate.CrackCrate;
import com.crackcrate.managers.CrateManager;
import com.crackcrate.models.CrateType;
import com.crackcrate.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CrateListener implements Listener {

    private final CrackCrate plugin;
    private final CrateManager crateManager;

    public CrateListener(CrackCrate plugin) {
        this.plugin = plugin;
        this.crateManager = plugin.getCrateManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only right clicks on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();

        // Check if this block is a registered crate location
        CrateType crate = crateManager.getCrateAtLocation(block.getLocation());
        if (crate == null) return;

        event.setCancelled(true);

        if (!player.hasPermission("crackcrate.use")) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfig().getString("messages.prefix", "") +
                    plugin.getConfig().getString("messages.no-permission", "&cSem permissão!")));
            return;
        }

        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("crackcrate.admin")) {
            // Admin in creative can preview
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfig().getString("messages.prefix", "") +
                    "&eVocê está em modo criativo. Use /crate open " + crate.getId() + " para abrir como admin."));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        // Check if holding a key
        String keyId = crateManager.getCrateIdFromKey(hand);
        if (keyId == null || !keyId.equals(crate.getId())) {
            player.sendMessage(ColorUtils.colorize(
                    plugin.getConfig().getString("messages.prefix", "") +
                    plugin.getConfig().getString("messages.no-key", "&cSem chave!")));
            return;
        }

        crateManager.openCrate(player, crate);
    }
}
