package net.Indyuce.inventory.compat.mmoitems;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.slot.CustomSlot;
import net.Indyuce.inventory.slot.restriction.SlotRestriction;
import net.Indyuce.inventory.util.LineConfig;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.ItemStack;

public class TypeRestriction extends SlotRestriction {

    /**
     * Forced to save the MMOItems type as a string and not a type instance
     * because the TypeManager has not been initialized yet... which is fine
     * because we don't need to get the Type instance for our checks
     */
    private final String[] typeIds;

    public TypeRestriction(LineConfig config) {
        config.validate("type");

        typeIds = config.getString("type").toUpperCase().replace("-", "_").replace(" ", "_").split("\\,");
    }

    @Override
    public boolean isVerified(InventoryHandler provider, CustomSlot slot, ItemStack item) {
        final NBTItem nbtItem = NBTItem.get(item);
        final Type type = Type.get(nbtItem.getString("MMOITEMS_ITEM_TYPE"));
        if (type == null)
            return false;

        for (String typeId : typeIds)
            if (typeId.equals(type.getId()))
                return true;
        return false;
    }
}
