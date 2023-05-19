package net.Indyuce.inventory.gui;

import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.api.event.ItemEquipEvent;
import net.Indyuce.inventory.inventory.CustomInventoryHandler;
import net.Indyuce.inventory.slot.CustomSlot;
import net.Indyuce.inventory.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class PlayerInventoryView implements InventoryHolder {
    private final CustomInventoryHandler targetData;
    private final Player player, target;

    private static final List<InventoryAction> supported = Arrays.asList(InventoryAction.PICKUP_ALL, InventoryAction.SWAP_WITH_CURSOR, InventoryAction.PLACE_ALL);

    public PlayerInventoryView(Player player) {
        this(player, player);
    }

    /**
     * @param player Player opening the GUI and manipulating the items
     * @param target The player owning the inventory
     */
    public PlayerInventoryView(Player player, Player target) {
        this.target = target;
        this.player = player;

        targetData = (CustomInventoryHandler) MMOInventory.plugin.getDataManager().get(target);
    }

    @Override
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(this, MMOInventory.plugin.inventorySlots, MMOInventory.plugin.getPlaceholderParser().parse(target, target.equals(player) ? MMOInventory.plugin.getTranslation("inventory-name.self") : MMOInventory.plugin.getTranslation("inventory-name.other").replace("{name}", target.getName())));

        // Load custom items or vanilla items depending on slot type
        for (CustomSlot slot : MMOInventory.plugin.getSlotManager().getLoaded()) {
            ItemStack item = targetData.getItem(slot);
            inv.setItem(slot.getIndex(), Utils.isAir(item) ? slot.displayItem(target) : item);
        }

        // Fill remaining inventory
        for (int j = 0; j < inv.getSize(); j++) {
            ItemStack item = inv.getItem(j);
            if (Utils.isAir(item) && MMOInventory.plugin.getSlotManager().hasFiller())
                inv.setItem(j, MMOInventory.plugin.getSlotManager().getFiller().displayItem(target));
        }

        return inv;
    }

    public void open() {
        player.openInventory(getInventory());
    }

    /**
     * This makes sure all the items placed in the current inventory MATCH EXACTLY
     * the items registered by MMOInventory. A priori this is true, but other plugins
     * like MMOItems gem stone application can modify items without telling MMOInventory.
     * <p>
     * This method can be used to synchronize items in MMOInventory.
     * <p>
     * This fixes https://gitlab.com/phoenix-dvpmt/mmoinventory/-/issues/71
     */
    public void synchronize(Inventory inv, boolean trustBukkit) {
        for (CustomSlot slot : MMOInventory.plugin.getSlotManager().getLoaded()) {
            final @Nullable ItemStack bukkitItem = realItemOrNull(inv.getItem(slot.getIndex()));
            final @Nullable ItemStack pluginItem = targetData.getItem(slot);
            final boolean match = bukkitItem == pluginItem || (bukkitItem != null && pluginItem != null && pluginItem.hashCode() == bukkitItem.hashCode() && pluginItem.equals(bukkitItem));
            if (match) continue;

            if (trustBukkit) targetData.setItem(slot, bukkitItem);
            else inv.setItem(slot.getIndex(), pluginItem);
        }
    }

    @Nullable
    private ItemStack realItemOrNull(@Nullable ItemStack item) {
        return item == null ? null : Utils.isGuiItem(item) ? null : item;
    }

    public void whenCanceled(InventoryClickEvent event) {
        synchronize(event.getInventory(), true);
    }

    public void whenClicked(InventoryClickEvent event) {

        synchronize(event.getInventory(), false);

        /*
         * Shift clicking finds the ONE slot that can host your item
         * and automatically sends it there. There's a distinction to
         * be made between clicking in the player inventory and clicking
         * on an item which has already been equipped and which has to
         * be sent back to the player's inventory.
         *
         * There's also a problem with item amounts? It's much simpler
         * to just place all the stacked items as it's anyways quite
         * rare to have accessories with max stacks size greater than 1
         */
        final ItemStack picked = event.getCurrentItem();
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);

            // Nothing to do
            if (Utils.isAir(picked) || Utils.isGuiItem(picked)) return;

            if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {

                // Find the best slot available
                final CustomSlot best = findBestSlot(picked);
                if (best == null) return;

                final ItemEquipEvent called = new ItemEquipEvent(target, picked, best, ItemEquipEvent.EquipAction.SHIFT_CLICK_EQUIP);
                Bukkit.getPluginManager().callEvent(called);
                if (called.isCancelled()) return;

                targetData.setItem(best, picked);
                event.getInventory().setItem(best.getIndex(), picked);
                event.setCurrentItem(null);

                // For all active watchers
                forEachWatcher(view -> view.getTopInventory().setItem(best.getIndex(), picked));

            } else {

                // Curse of Binding
                if (picked != null && !canBeMoved(picked)) {
                    event.setCancelled(true);
                    return;
                }

                // Get the clicked slot
                final CustomSlot slot = MMOInventory.plugin.getSlotManager().get(event.getRawSlot());
                if (slot == null) return;

                // Find a place where to put the item
                int empty = player.getInventory().firstEmpty();
                if (empty == -1) return;

                final ItemEquipEvent called = new ItemEquipEvent(target, picked, null, ItemEquipEvent.EquipAction.SHIFT_CLICK_UNEQUIP);
                Bukkit.getPluginManager().callEvent(called);
                if (called.isCancelled()) return;

                targetData.setItem(slot, null);
                player.getInventory().setItem(empty, picked);
                event.setCurrentItem(slot.displayItem(target));

                // For all active watchers
                forEachWatcher(view -> view.getTopInventory().setItem(slot.getIndex(), slot.displayItem(target)));
            }

            return;
        }

        // Only a few types of clicks are supported
        if (!supported.contains(event.getAction())) {
            event.setCancelled(true);
            return;
        }

        // Prevent any interaction with filler slots
        if (Utils.isGuiItem(picked) && Utils.getGuiItemId(picked).equals("fill")) {
            event.setCancelled(true);
            return;
        }

        // Player tries to pickup a slot item, without equipping any
        if (Utils.isGuiItem(picked) && Utils.isAir(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        // Check for clicked slot
        final CustomSlot slot = MMOInventory.plugin.getSlotManager().get(event.getRawSlot());
        if (slot == null) {

            // Prevent from interacting with empty spaces
            if (event.getClickedInventory().equals(event.getInventory())) event.setCancelled(true);

            return;
        }

        // Curse of Binding
        if (picked != null && !canBeMoved(picked)) {
            event.setCancelled(true);
            return;
        }

        // Check if item can be equipped (apply slot restrictions)
        final ItemStack cursor = event.getCursor();
        if (!Utils.isAir(event.getCursor())) {

            // Prevents equipping stacked items
            if (MMOInventory.plugin.getConfig().getBoolean("disable-equiping-stacked-items", true) && event.getCursor().getAmount() > 1) {
                event.setCancelled(true);
                return;
            }

            // Check for vanilla AND custom slot restrictions
            if (!slot.canHost(targetData, cursor)) {
                event.setCancelled(true);
                return;
            }
        }

        /*
         * May be called with a null item as parameter if the player is
         * unequipping an item
         */
        final ItemEquipEvent.EquipAction action = Utils.isAir(event.getCursor()) ? ItemEquipEvent.EquipAction.UNEQUIP : Utils.isGuiItem(cursor) ? ItemEquipEvent.EquipAction.EQUIP : ItemEquipEvent.EquipAction.SWAP_ITEMS;
        final ItemEquipEvent equipEvent = new ItemEquipEvent(target, event.getCursor(), slot, action);
        Bukkit.getPluginManager().callEvent(equipEvent);
        if (equipEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        targetData.setItem(slot, event.getCursor());

        // For all active watchers
        final ItemStack newItem = Utils.isAir(event.getCursor()) ? slot.displayItem(target) : event.getCursor();
        forEachWatcher(view -> view.getTopInventory().setItem(slot.getIndex(), newItem));

        /*
         * If the player has picked up an inventory slot item, remove it
         * instantly after checking the equip event was not canceled (bug
         * fix)
         */
        if (Utils.isGuiItem(picked)) event.setCurrentItem(null);

        /*
         * If the player is taking away an item without swapping it, place
         * the inventory slot item back in the corresponding slot.
         *
         * This must be done using a delayed task otherwise this will replace
         * the event's current item
         */
        if (Utils.isAir(event.getCursor()))
            Bukkit.getScheduler().runTask(MMOInventory.plugin, () -> event.getInventory().setItem(slot.getIndex(), slot.displayItem(target)));

        // Finally update the player's inventory
        MMOInventory.plugin.updateInventory(player);
    }

    /**
     * @return True if item does not have Curse of Binding
     */
    private boolean canBeMoved(@NotNull ItemStack item) {
        return !item.hasItemMeta() || !item.getItemMeta().hasEnchant(Enchantment.BINDING_CURSE);
    }

    /**
     * Used when shift clicking to find the best slot
     * available for a specific item.
     *
     * @param item The item being shift clicked
     * @return The best slot available for that item, or none if there isn't any.
     *         Shift clicking should not do anything then
     */
    private CustomSlot findBestSlot(ItemStack item) {
        for (CustomSlot slot : MMOInventory.plugin.getSlotManager().getLoaded())
            if ((slot.getType().isCustom() || slot.getType().getVanillaSlotHandler().supportsShiftClick()) && !targetData.hasItem(slot) && slot.canHost(targetData, item))
                return slot;

        return null;
    }

    /**
     * This is used when an admin edits the inventory of another
     * player, while the target player COULD have his inventory opened
     * as well.
     * <p>
     * This method iterates through all the online players whose
     * open inventory matches the target's rpg inventory
     */
    public void forEachWatcher(Consumer<InventoryView> consumer) {
        for (Player online : Bukkit.getOnlinePlayers())
            if (online.getOpenInventory() != null && online.getOpenInventory().getTopInventory().getHolder() instanceof PlayerInventoryView) {
                PlayerInventoryView customGui = (PlayerInventoryView) online.getOpenInventory().getTopInventory().getHolder();
                if (!equals(customGui) && customGui.target.equals(target)) consumer.accept(online.getOpenInventory());
            }
    }
}
