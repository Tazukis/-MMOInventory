package net.Indyuce.inventory.compat.mmoitems;

import io.lumine.mythic.lib.api.player.EquipmentSlot;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.api.event.ItemEquipEvent;
import net.Indyuce.inventory.compat.InventoryUpdater;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.inventory.InventoryItem;
import net.Indyuce.inventory.inventory.InventoryLookupMode;
import net.Indyuce.inventory.slot.CustomSlot;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.inventory.EquippedItem;
import net.Indyuce.mmoitems.comp.inventory.PlayerInventory;
import net.Indyuce.mmoitems.comp.inventory.RPGInventoryHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * No need to implement {@link PlayerInventory} because MMOItems
 * already consider RPGInventory to be installed. Therefore we
 * have to consider that an instance of {@link RPGInventoryHook}
 * is already registered in MMOItems
 */
public class MMOItemsCompatibility implements Listener, InventoryUpdater, PlayerInventory {
    public MMOItemsCompatibility() {

        // Disable default compatibility with RPGInv
        MMOItems.plugin.getInventory().unregisterIf(inv -> inv instanceof RPGInventoryHook);

        // Special compatibility with MMOItems
        MMOItems.plugin.getInventory().register(this);
        MMOInventory.plugin.registerInventoryUpdater(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void updateWhenEquippingItem(ItemEquipEvent event) {
        Bukkit.getScheduler().runTaskLater(MMOInventory.plugin, () -> PlayerData.get(event.getPlayer()).updateInventory(), 0);
    }

    @Override
    public void updateInventory(Player player) {
        PlayerData.get(player).updateInventory();
    }

    @Override
    public List<EquippedItem> getInventory(Player player) {
        final List<EquippedItem> list = new ArrayList<>();
        final InventoryHandler inv = MMOInventory.plugin.getDataManager().get(player.getUniqueId());

        for (InventoryItem invItem : inv.getItems(InventoryLookupMode.NORMAL))
            list.add(new CustomEquippedItem(inv, invItem.getSlot(), invItem.getItemStack()));

        return list;
    }

    public class CustomEquippedItem extends EquippedItem {
        private final InventoryHandler holder;
        private final CustomSlot customSlot;

        public CustomEquippedItem(InventoryHandler holder, CustomSlot customSlot, ItemStack item) {
            super(item, EquipmentSlot.ACCESSORY);

            this.holder = holder;
            this.customSlot = customSlot;
        }

        @Override
        public void setItem(@Nullable ItemStack itemStack) {
            holder.setItem(customSlot, itemStack);
        }
    }
}
