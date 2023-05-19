package net.Indyuce.inventory.manager.data;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import io.lumine.mythic.lib.comp.profile.DefaultProfileDataModule;
import io.lumine.mythic.lib.data.DefaultOfflineDataHolder;
import io.lumine.mythic.lib.data.SynchronizedDataManager;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.CustomInventoryHandler;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.manager.data.yaml.YAMLDataHandler;
import org.apache.commons.lang3.Validate;

import java.util.function.Function;

public class PlayerDataManager extends SynchronizedDataManager<InventoryHandler, DefaultOfflineDataHolder> {

    /**
     * Function used to generate an inventoryProvider instance given a player.
     * There are two types of inventoryProviders: Complex, which are used when
     * the custom inventory is used, and Simple when the 'no-custom-inventory'
     * is toggled on
     */
    private Function<MMOPlayerData, InventoryHandler> load = (playerData -> new CustomInventoryHandler(playerData));

    public PlayerDataManager() {
        super(MMOInventory.plugin, new YAMLDataHandler());
    }

    /**
     * Used by the 'no-custom-inventory' config option
     *
     * @param load A function which takes a player as input and returns a new
     *             inventoryProvider corresponding to the player
     */
    public void setInventoryProvider(Function<MMOPlayerData, InventoryHandler> load) {
        Validate.notNull(load, "Function cannot be null");

        this.load = load;
    }

    @Override
    public InventoryHandler newPlayerData(MMOPlayerData mmoPlayerData) {
        return load.apply(mmoPlayerData);
    }

    @Override
    public Object newProfileDataModule() {
        return new DefaultProfileDataModule(MMOInventory.plugin);
    }
}
