package net.Indyuce.inventory.manager.data.yaml;

import io.lumine.mythic.lib.data.DefaultOfflineDataHolder;
import io.lumine.mythic.lib.data.yaml.YAMLSynchronizedDataHandler;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.CustomInventoryHandler;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.slot.CustomSlot;
import net.Indyuce.inventory.util.Utils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.logging.Level;

public class YAMLDataHandler extends YAMLSynchronizedDataHandler<InventoryHandler, DefaultOfflineDataHolder> {
    public YAMLDataHandler() {
        super(MMOInventory.plugin);
    }

    @Override
    public void setup() {

    }

    @Override
    public void saveInSection(InventoryHandler data, ConfigurationSection config) {
        if (!(data instanceof CustomInventoryHandler)) return;

        final CustomInventoryHandler custom = (CustomInventoryHandler) data;
        config.set("inventory", null);

        for (int index : custom.getFilledSlotKeys()) {
            final ItemStack item = custom.getItem(index);
            config.set("inventory." + index, Utils.isAir(item) ? null : item);
        }
    }

    @Override
    public void loadFromSection(InventoryHandler data, ConfigurationSection config) {
        if (!(data instanceof CustomInventoryHandler)) return;

        if (config.contains("inventory")) for (String key : config.getConfigurationSection("inventory").getKeys(false))
            try {
                CustomSlot customSlot = MMOInventory.plugin.getSlotManager().get(Integer.parseInt(key));
                data.setItem(customSlot, config.getItemStack("inventory." + key));
            } catch (IllegalArgumentException exception) {
                MMOInventory.plugin.getLogger().log(Level.SEVERE, "Could not read inventory item indexed " + key + " of " + data.getPlayer().getName() + ": " + exception.getMessage());
            }
    }

    @Override
    public DefaultOfflineDataHolder getOffline(UUID uuid) {
        return new DefaultOfflineDataHolder(uuid);
    }

    @Override
    public void close() {

    }
}
