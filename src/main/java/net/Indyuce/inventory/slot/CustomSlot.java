package net.Indyuce.inventory.slot;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.lumine.mythic.lib.UtilityMethods;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.slot.restriction.SlotRestriction;
import net.Indyuce.inventory.util.LineConfig;
import org.apache.commons.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CustomSlot {
    private final String id;
    private final SlotType type;
    private final int slot;
    private final ItemStack item;

    /**
     * Slot restrictions used by external plugins to apply type or class
     * restrictions for instance
     */
    private final List<SlotRestriction> restrictions = new ArrayList<>();

    /**
     * Used to register custom RPG inventory slots from other plugins
     *
     * @param id   The custom slot id (CHESTPLATE)
     * @param name The custom slot name ("Chestplate")
     * @param type The slot type, use the corresponding type for vanilla slots,
     *             ACCESSORY for custom RPG slots, or FILL for filler items
     * @param slot The GUI slot that will be used to display the current item in
     *             /rpginv
     * @param item The itemstack used to indicate the custom slot in the GUI
     * @deprecated Parameter <code>name</code> is useless
     */
    @Deprecated
    public CustomSlot(String id, String name, SlotType type, int slot, ItemStack item) {
        this(id, type, slot, item);
    }

    /**
     * Used to register custom RPG inventory slots from other plugins
     *
     * @param id   The custom slot id (CHESTPLATE)
     * @param type The slot type, use the corresponding type for vanilla slots,
     *             ACCESSORY for custom RPG slots, or FILL for filler items
     * @param slot The GUI slot that will be used to display the current item in
     *             /rpginv
     * @param item The itemstack used to indicate the custom slot in the GUI
     */
    public CustomSlot(String id, SlotType type, int slot, ItemStack item) {
        this.id = id;
        this.type = type;
        this.item = item;

        // This makes sure any fill slot does not interfere with other slots
        this.slot = type == SlotType.FILL ? -1 : slot;
    }

    public CustomSlot(ConfigurationSection config) {
        Validate.notNull(config, "Could not read slot config");
        id = config.getName().toLowerCase().replace("_", "-").replace(" ", "-");

        Validate.notNull(config.getString("type"), "Could not read slot type");
        type = SlotType.valueOf(config.getString("type").toUpperCase().replace("-", "_").replace(" ", "_"));
        slot = type == SlotType.FILL ? -1 : config.getInt("slot");

        // Cache slot item
        int model = config.getInt("durability", config.getInt("custom-model-data"));
        this.item = new ItemStack(Material.valueOf(UtilityMethods.enumName(Objects.requireNonNull(config.getString("material"), "Could not read slot material"))));
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(model);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(config.getString("name"), "Could not read slot name")));
        meta.addItemFlags(ItemFlag.values());
        List<String> lore = new ArrayList<>();
        for (String line : Objects.requireNonNull(config.getStringList("lore"), "Could not read slot lore"))
            lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', line));
        meta.setLore(lore);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(new NamespacedKey(MMOInventory.plugin, "GuiItemId"), PersistentDataType.STRING, getId());
        if (config.contains("texture-value"))
            setTextureValue(meta, config.getString("texture-value"));
        item.setItemMeta(meta);

        // load slot restrictions
        if (config.contains("restrictions"))
            for (String key : config.getStringList("restrictions"))
                restrictions.add(MMOInventory.plugin.getSlotManager().readRestriction(new LineConfig(key)));
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return slot;
    }

    public SlotType getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    /**
     * The returned item does not automatically updates when a placeholder value
     * changes of course. The main problem with this implementation is that most
     * GUIs calculate the item once and don't keep it updated every x tick or something.
     *
     * @param player Player to which MMOInv is displaying the item
     * @return Displays an item to a player. Same output as {@link #getItem()}
     *         but with placeholder support in the item display name and lore.
     *         <p>
     *         Besides it's not guaranteed to be a clone of the item
     */
    public ItemStack displayItem(Player player) {
        if (!item.hasItemMeta())
            return item;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore())
            return item;

        // Change lore
        List<String> lore = meta.getLore();
        for (int j = 0; j < lore.size(); j++)
            lore.set(j, MMOInventory.plugin.getPlaceholderParser().parse(player, lore.get(j)));
        meta.setLore(lore);

        // Generate item
        ItemStack clone = item.clone();
        clone.setItemMeta(meta);
        return clone;
    }

    /**
     * @param player Data of the player equipping the item
     * @param item   The item being equipped in the slot
     * @return If the item can be equipped in that slot. This only checks for
     *         custom restrictions and NOT for vanilla slot based restrictions.
     *         See {@link #canHost(InventoryHandler, ItemStack)}
     */
    public boolean checkSlotRestrictions(InventoryHandler player, ItemStack item) {
        for (SlotRestriction restriction : restrictions)
            if (!restriction.isVerified(player, this, item))
                return false;

        return true;
    }

    /**
     * The best method to check if an item can host another item. This can
     * be used for BOTH vanilla slot and custom slots!
     *
     * @param player Data of the player equipping the item
     * @param item   The item being equipped in the slot
     * @return If the item can be equipped in that slot. This checks for
     *         both custom AND vanilla slot restrictions
     */
    public boolean canHost(InventoryHandler player, ItemStack item) {
        return getType().isCustom() ? checkSlotRestrictions(player, item) : getType().getVanillaSlotHandler().canEquip(item);
    }

    private void setTextureValue(ItemMeta meta, String textureValue) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", textureValue));
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalArgumentException("Could not apply skull texture '" + textureValue + "':" + exception.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomSlot that = (CustomSlot) o;
        return slot == that.slot;
    }

    @Override
    public int hashCode() {
        return Objects.hash(slot);
    }
}
