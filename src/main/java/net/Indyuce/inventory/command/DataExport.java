package net.Indyuce.inventory.command;

import java.text.DecimalFormat;

/**
 * Centralize into MythicLib
 */
@Deprecated
public class DataExport {

    /**
     * Amount of requests generated every batch
     */
    private static final int BATCH_AMOUNT = 50;

    /**
     * Period in ticks
     */
    private static final int BATCH_PERIOD = 20;

    private static final DecimalFormat decFormat = new DecimalFormat("0.#");

    /*public boolean execute(CommandSender sender) {

        if (!MMOInventory.plugin.getDataManager().getLoaded().isEmpty()) {
            sender.sendMessage("Please make sure no players are logged in when using this command. " +
                    "If you are still seeing this message, restart your server and execute this command before any player logs in.");
            return false;
        }

        final List<UUID> playerIds = Arrays.stream(new File(MMOInventory.plugin.getDataFolder() + "/userdata").listFiles())
                .map(file -> UUID.fromString(file.getName().replace(".yml", ""))).collect(Collectors.toList());

        // Initialize fake SQL data provider
        final SQLDataManager dataManager;
        try {
            dataManager = new SQLDataManager();
        } catch (RuntimeException exception) {
            sender.sendMessage("Could not initialize SQL provider (see console for stack trace): " + exception.getMessage());
            return false;
        }

        final double timeEstimation = (double) playerIds.size() / BATCH_AMOUNT * BATCH_PERIOD / 20;
        sender.sendMessage("Exporting " + playerIds.size() + " player data(s).. See console for details");
        sender.sendMessage("Minimum expected time: " + decFormat.format(timeEstimation) + "s");

        // Save player data
        new BukkitRunnable() {
            int errorCount = 0;
            int batchCounter = 0;

            @Override
            public void run() {
                for (int i = 0; i < BATCH_AMOUNT; i++) {
                    final int index = BATCH_AMOUNT * batchCounter + i;

                    /*
                     * Saving is done. Close connection to avoid memory
                     * leaks and ouput the results to the command executor
                     */
                    /*if (index >= playerIds.size()) {
                        cancel();

                        dataManager.getDataSource().close();
                        MMOInventory.plugin.getLogger().log(Level.WARNING, "Exported " + playerIds.size() + " player datas to SQL database. Total errors: " + errorCount);
                        return;
                    }

                    final UUID playerId = playerIds.get(index);
                    try {
                        final CustomInventoryHandler offlinePlayerData = new CustomInventoryHandler(playerId);
                        MMOInventory.plugin.getDataManager().setupData(offlinePlayerData);

                        // Player data is loaded, now it gets saved through SQL
                        sqlProvider.getDataManager().saveData(offlinePlayerData, true);
                    } catch (RuntimeException exception) {
                        errorCount++;
                        exception.printStackTrace();
                    }
                }

                batchCounter++;
            }
        }.runTaskTimerAsynchronously(MMOCore.plugin, 0, BATCH_PERIOD);

        return true;
    }
                        */
}
