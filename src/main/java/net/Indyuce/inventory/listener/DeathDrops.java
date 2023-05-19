package net.Indyuce.inventory.listener;

import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.CustomInventoryHandler;
import net.Indyuce.inventory.slot.CustomSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class DeathDrops implements Listener {

    @EventHandler
    public void dropItemsOnDeath(PlayerDeathEvent event) {
        if (event.getKeepInventory())
            return;

        final @Nullable CustomInventoryHandler data = (CustomInventoryHandler) MMOInventory.plugin.getDataManager().getOrNull(event.getEntity());
        if (data == null)
            return;

        for (CustomSlot slot : data.getFilledSlots()) {
            ItemStack item = data.getItem(slot);
            data.setItem(slot, null);
            event.getEntity().getWorld().dropItem(event.getEntity().getLocation(), item);
        }
    }
}
