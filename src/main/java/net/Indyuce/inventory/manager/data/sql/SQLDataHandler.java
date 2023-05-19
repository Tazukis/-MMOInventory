package net.Indyuce.inventory.manager.data.sql;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.data.DefaultOfflineDataHolder;
import io.lumine.mythic.lib.data.sql.SQLDataSource;
import io.lumine.mythic.lib.data.sql.SQLDataSynchronizer;
import io.lumine.mythic.lib.data.sql.SQLSynchronizedDataHandler;
import net.Indyuce.inventory.MMOInventory;
import net.Indyuce.inventory.inventory.*;
import net.Indyuce.inventory.manager.data.yaml.YAMLDataHandler;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SQLDataHandler extends SQLSynchronizedDataHandler<InventoryHandler, DefaultOfflineDataHolder, MMOInventoryDataSynchronizer> {
    public SQLDataHandler(SQLDataSource dataSource) {
        super(dataSource);
    }

    private static final String[] NEW_COLUMNS = new String[]{
            "is_saved", "TINYINT"};

    @Override
    public void setup() {
        getDataSource().executeUpdateAsync("CREATE TABLE IF NOT EXISTS mmoinventory_inventories (" +
                "uuid VARCHAR(36) NOT NULL," +
                "inventory LONGTEXT," +
                "is_saved TINYINT," +
                "PRIMARY KEY (uuid));");

        // Nullable inventory
        getDataSource().executeUpdate("ALTER TABLE `mmoinventory_inventories` MODIFY `inventory` LONGTEXT;");

        // Add columns that might not be here by default
        for (int i = 0; i < NEW_COLUMNS.length; i += 2) {
            final String columnName = NEW_COLUMNS[i];
            final String dataType = NEW_COLUMNS[i + 1];
            getDataSource().getResultAsync("SELECT * FROM information_schema.COLUMNS WHERE TABLE_NAME = 'mmoinventory_inventories' AND COLUMN_NAME = '" + columnName + "'", result -> {
                try {
                    if (!result.next())
                        getDataSource().executeUpdate("ALTER TABLE mmoinventory_inventories ADD COLUMN " + columnName + " " + dataType);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                }
            });
        }
    }

    @Override
    public CompletableFuture<Void> loadData(InventoryHandler playerData) {
        if (!(playerData instanceof CustomInventoryHandler)) return CompletableFuture.runAsync(() -> {
        });
        return super.loadData(playerData);
    }

    @Override
    public SQLDataSynchronizer newDataSynchronizer(InventoryHandler data) {
        return new MMOInventoryDataSynchronizer(getDataSource(), (CustomInventoryHandler) data);
    }

    @Override
    public void saveData(InventoryHandler data, boolean autosave) {

        if (!(data instanceof CustomInventoryHandler))
            return;

        try {

            final Collection<InventoryItem> items = data.getItems(InventoryLookupMode.NORMAL);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final BukkitObjectOutputStream bukkitAdapter = new BukkitObjectOutputStream(outputStream);

            // Serialize items
            bukkitAdapter.writeInt(items.size());
            for (InventoryItem entry : items) {
                final int slot = entry.getSlot().getIndex();
                bukkitAdapter.writeInt(slot);
                bukkitAdapter.writeObject(entry.getItemStack());
            }

            // Encode serialized object into to the Base64 format and execute update
            bukkitAdapter.close();
            final String base64 = new String(Base64.getEncoder().encode(outputStream.toByteArray()));

            CompletableFuture.runAsync(() -> {
                try {
                    final Connection connection = getDataSource().getConnection();
                    final PreparedStatement prepared = connection.prepareStatement("INSERT INTO mmoinventory_inventories (uuid, inventory, `is_saved`) VALUES(?,?,?) ON DUPLICATE KEY UPDATE inventory = VALUES(`inventory`), `is_saved` = VALUES(`is_saved`);");

                    try {
                        UtilityMethods.debug(MMOInventory.plugin, "SQL", "Saving data of '" + data.getProfileId() + "'");
                        prepared.setString(1, data.getProfileId().toString());
                        prepared.setString(2, base64);
                        prepared.setInt(3, autosave ? 0 : 1);
                        prepared.executeUpdate();
                        UtilityMethods.debug(MMOInventory.plugin, "SQL", "Saved data of '" + data.getProfileId() + "'");
                    } catch (Throwable throwable) {
                        MMOInventory.plugin.getLogger().log(Level.WARNING, "Could not save player inventory of " + data.getPlayer().getName() + " (" + data.getProfileId() + ")");
                        throwable.printStackTrace();
                    } finally {

                        // Close statement and connection to prevent leaks
                        prepared.close();
                        connection.close();
                    }
                } catch (SQLException exception) {
                    MMOInventory.plugin.getLogger().log(Level.WARNING, "Could not save player inventory of " + data.getPlayer().getName() + " (" + data.getProfileId() + "), saving in YAML instead");
                    exception.printStackTrace();

                    // Save in YAML
                    new YAMLDataHandler().saveData(data, autosave);
                }
            });

        } catch (IOException exception) {
            MMOInventory.plugin.getLogger().log(Level.WARNING, "Could not save player inventory of " + data.getPlayer().getName() + " (" + data.getProfileId() + ")");
            exception.printStackTrace();
        }
    }

    @Override
    public DefaultOfflineDataHolder getOffline(UUID uuid) {
        return new DefaultOfflineDataHolder(uuid);
    }
}
