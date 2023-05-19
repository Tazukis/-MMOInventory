package net.Indyuce.inventory.inventory;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.slot.CustomSlot;
import net.Indyuce.inventory.util.Utils;
import org.apache.commons.lang3.Validate;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomInventoryHandler extends InventoryHandler {

	/**
	 * Items are stored inside a map with their corresponding slot because it is
	 * much easier to access every item at the same time. It also makes items
	 * accessible and EDITABLE by any other plugins.
	 */
	private final Map<Integer, InventoryItem> items = new HashMap<>();

	/**
	 * Used when MMOInventory utilizes the custom inventory GUI
	 */
	public CustomInventoryHandler(MMOPlayerData playerData) {
		super(playerData);
	}

	@Override
	protected void insertItemAtSlot(@NotNull CustomSlot slot, @Nullable ItemStack item) {
		Validate.isTrue(slot.getType().isCustom(), "Not a custom slot");

		/*
		 * Map containing no NULL values makes it much easier for external
		 * plugins to manipulate the mapped itemStacks
		 */
		if (Utils.isAir(item))
			items.remove(slot.getIndex());

			/*
			 * Equip item i.e add a clone to the map. The item is cloned so that
			 * further modifications do not impact the instance stored in the
			 * custom inventory
			 */
		else
			items.put(slot.getIndex(), new InventoryItem(item.clone(), slot));
	}

	@Override
	protected Collection<InventoryItem> retrieveItems() {
		return items.values();
	}

	/**
	 * For custom slots, this checks for a key in the player's item map.
	 * For vanilla slots, this reaches in the player's inventory and see
	 * if it has equipped an item in the corresponding slot.
	 *
	 * @param slot Custom slot to check
	 * @return The item equipped by the player
	 */
	@Nullable
	public ItemStack getItem(CustomSlot slot) {
		return slot.getType().isCustom() ? getItem(slot.getIndex()) : slot.getType().getVanillaSlotHandler().retrieveItem(getPlayer());
	}

	@Nullable
	public ItemStack getItem(int slot) {
		InventoryItem invItem = items.get(slot);
		return invItem == null ? null : invItem.getItemStack();
	}

	/**
	 * For custom slots, this checks for a key in the player's item map.
	 * For vanilla slots, this reaches in the player's inventory and see
	 * if it has equipped an item in the corresponding slot.
	 *
	 * @param slot Custom slot to check
	 * @return If the player has an item in a specific slot
	 */
	public boolean hasItem(CustomSlot slot) {
		return slot.getType().isCustom() ? items.containsKey(slot.getIndex()) : !Utils.isAir(slot.getType().getVanillaSlotHandler().retrieveItem(getPlayer()));
	}

	public Set<Integer> getFilledSlotKeys() {
		return items.keySet();
	}

	public Set<CustomSlot> getFilledSlots() {
		return items.keySet().stream().map(id -> MMOInventory.plugin.getSlotManager().get(id)).collect(Collectors.toSet());
	}
}
