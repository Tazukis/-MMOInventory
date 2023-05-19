package net.Indyuce.inventory.manager.data.sql;

import io.lumine.mythic.lib.data.sql.SQLDataSource;
import io.lumine.mythic.lib.data.sql.SQLDataSynchronizer;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.CustomInventoryHandler;
import net.Indyuce.inventory.inventory.InventoryHandler;
import net.Indyuce.inventory.slot.CustomSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

public class MMOInventoryDataSynchronizer extends SQLDataSynchronizer<InventoryHandler> {
    public MMOInventoryDataSynchronizer(SQLDataSource dataSource, CustomInventoryHandler data) {
        super("mmoinventory_inventories", "uuid", dataSource, data);
    }

    @Override
    public void loadData(ResultSet result) throws SQLException, IOException, ClassNotFoundException {

        // Decode serialized object
        final @Nullable String columnValue = result.getString("inventory");
        if (columnValue != null && !columnValue.isEmpty()) {
            final byte[] serializedObject = Base64.getDecoder().decode(columnValue);
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(serializedObject);
            final BukkitObjectInputStream bukkitAdapter = new BukkitObjectInputStream(inputStream);

            int avail = bukkitAdapter.readInt();
            while (avail-- > 0) {
                final int slot = bukkitAdapter.readInt();
                final ItemStack stack = (ItemStack) bukkitAdapter.readObject();
                final CustomSlot customSlot = MMOInventory.plugin.getSlotManager().get(slot);
                getData().setItem(customSlot, stack);
            }

            bukkitAdapter.close();
        }
    }

    @Override
    public void loadEmptyData() {
        // Nothing to do
    }
}
