package net.Indyuce.inventory.inventory;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.data.SynchronizedDataHolder;
import net.Indyuce.inventory.slot.CustomSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class InventoryHandler extends SynchronizedDataHolder {
    public InventoryHandler(MMOPlayerData playerData) {
        super(playerData);
    }

    public void setItem(@NotNull CustomSlot slot, @Nullable ItemStack item) {
        if (slot.getType().isCustom())
            insertItemAtSlot(slot, item);
        else
            slot.getType().getVanillaSlotHandler().equip(getPlayer(), item);
    }

    /**
     * @param lookupMode The way MMOInv collects and filters
     *                   items in the returned collection
     * @return The extra items from the player's custom inventory
     */
    public Collection<InventoryItem> getItems(InventoryLookupMode lookupMode) {
        Set<InventoryItem> items = new HashSet<>();
        for (InventoryItem invItem : retrieveItems())
            if (lookupMode == InventoryLookupMode.IGNORE_RESTRICTIONS || invItem.getSlot().checkSlotRestrictions(this, invItem.getItemStack()))
                items.add(invItem);
        return items;
    }

    /**
     * Puts an item in a specific slot index. This works for both
     * slots in the custom inventory GUI, and slot indexes from
     * the player's inventory as long as they are nicely configured.
     *
     * @param slot Target slot
     * @param item Item to place
     */
    protected abstract void insertItemAtSlot(@NotNull CustomSlot slot, @Nullable ItemStack item);

    /**
     * @return A collection of all the extra items (vanilla slots put aside) ie
     *         accessories placed in custom RPG slots. This should include all items even
     *         the ones not usable by the player.
     */
    protected abstract Collection<InventoryItem> retrieveItems();
}
