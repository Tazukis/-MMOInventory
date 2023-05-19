package net.Indyuce.inventory;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.data.sql.SQLDataSource;
import io.lumine.mythic.lib.version.SpigotPlugin;
import net.Indyuce.inventory.command.MMOInventoryCommand;
import net.Indyuce.inventory.command.MMOInventoryCompletion;
import net.Indyuce.inventory.compat.*;
import net.Indyuce.inventory.compat.list.DefaultHook;
import net.Indyuce.inventory.compat.mmoitems.MMOItemsCompatibility;
import net.Indyuce.inventory.compat.mmoitems.TypeRestriction;
import net.Indyuce.inventory.compat.mmoitems.UniqueRestriction;
import net.Indyuce.inventory.compat.mmoitems.UseRestriction;
import net.Indyuce.inventory.compat.placeholder.DefaultParser;
import net.Indyuce.inventory.compat.placeholder.PlaceholderAPIParser;
import net.Indyuce.inventory.compat.placeholder.PlaceholderParser;
import net.Indyuce.inventory.gui.PlayerInventoryView;
import net.Indyuce.inventory.inventory.SimpleInventoryHandler;
import net.Indyuce.inventory.listener.*;
import net.Indyuce.inventory.manager.SlotManager;
import net.Indyuce.inventory.manager.data.PlayerDataManager;
import net.Indyuce.inventory.manager.data.sql.SQLDataHandler;
import net.Indyuce.inventory.util.ConfigFile;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.endlesscode.rpginventory.RPGInventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MMOInventory extends RPGInventory implements Listener {
    public static MMOInventory plugin;

    private final SlotManager slotManager = new SlotManager();

    /**
     * See {@link InventoryUpdater} for explanation. This is
     * the list of all the plugins which require inventory updates.
     */
    private final List<InventoryUpdater> inventoryUpdaters = new ArrayList<>();

    private LevelModule levelModule;
    private ClassModule classModule;
    private final PlayerDataManager playerDataManager;
    private ConfigFile language;
    private PlaceholderParser placeholderParser = new DefaultParser();

    // Cached config options
    public int inventorySlots;
    public boolean debugMode;

    public MMOInventory() {
        plugin = this;
        playerDataManager = new PlayerDataManager();
    }

    public void onLoad() {
        getLogger().log(Level.INFO, "Plugin file is called '" + getFile().getName() + "'");

        if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
            slotManager.registerRestriction(TypeRestriction::new, "mmoitemstype", "mmoitemtype", "mitype");
            slotManager.registerRestriction(config -> new UseRestriction(), "mmoitemslevel", "mmoitemlevel", "milevel");
            slotManager.registerRestriction(UniqueRestriction::new, "unique");
        }
    }

    public void onEnable() {

        saveDefaultConfig();
        saveDefaultFile("language");
        saveDefaultFile("items");
        reload();

        // Startup plugin metrics
        // new Metrics(this, 99445);

        // Update check
        new SpigotPlugin(99445, this).checkForUpdate();

        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getPluginManager().registerEvents(new GuiListener(), this);

        // MySQL usage
        if (getConfig().getBoolean("mysql.enabled")) {
            final SQLDataSource dataSource = new SQLDataSource(this);
            playerDataManager.setDataHandler(new SQLDataHandler(dataSource));
        }

        // Load class module
        try {
            String moduleName = getConfig().getString("class-module");
            ModuleType moduleType = ModuleType.valueOf(UtilityMethods.enumName(moduleName));
            Validate.isTrue(moduleType.findPlugin(), "Plugin '" + moduleType.name() + "'not installed");
            Object module = moduleType.getModule();
            Validate.isTrue(module instanceof ClassModule, "Plugin '" + moduleType.name() + "' does not support classes");
            this.classModule = (ClassModule) module;
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Could not initialize custom class module: " + exception.getMessage());
            this.classModule = new DefaultHook();
        }

        // Load level module
        try {
            String moduleName = getConfig().getString("level-module");
            ModuleType moduleType = ModuleType.valueOf(UtilityMethods.enumName(moduleName));
            Validate.isTrue(moduleType.findPlugin(), "Plugin '" + moduleType.name() + "'not installed");
            Object module = moduleType.getModule();
            Validate.isTrue(module instanceof LevelModule, "Plugin '" + moduleType.name() + "' does not support levels");
            this.levelModule = (LevelModule) module;
        } catch (Exception exception) {
            getLogger().log(Level.WARNING, "Could not initialize custom level module: " + exception.getMessage());
            this.levelModule = new DefaultHook();
        }

        if (getConfig().getBoolean("resource-pack.enabled"))
            Bukkit.getServer().getPluginManager().registerEvents(new ResourcePack(getConfig().getConfigurationSection("resource-pack")), this);

        if (getConfig().getBoolean("no-custom-inventory")) {
            playerDataManager.setInventoryProvider(SimpleInventoryHandler::new);
            Bukkit.getPluginManager().registerEvents(new NoCustomInventory(), this);
        } else {

            if (getConfig().getBoolean("inventory-button.enabled"))
                Bukkit.getPluginManager().registerEvents(new InventoryButtonListener(getConfig().getConfigurationSection("inventory-button")), this);

            if (getConfig().getBoolean("drop-on-death"))
                Bukkit.getServer().getPluginManager().registerEvents(new DeathDrops(), this);
        }

        getCommand("mmoinventory").setExecutor(new MMOInventoryCommand());
        getCommand("mmoinventory").setTabCompleter(new MMOInventoryCompletion());

        // Setup data for online players
        playerDataManager.initialize(EventPriority.NORMAL, EventPriority.NORMAL);

        if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
            new MMOItemsCompatibility();
            getLogger().log(Level.INFO, "Hooked onto MMOItems");
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderParser = new PlaceholderAPIParser();
            getLogger().log(Level.INFO, "Hooked onto PlaceholderAPI");
        }

        if (getServer().getPluginManager().getPlugin("Oraxen") != null) {
            new OraxenIntegration();
            getLogger().log(Level.INFO, "Hooked onto Oraxen");
        }
    }

    public void onDisable() {

        // Close open inventories
        for (Player player : Bukkit.getOnlinePlayers())
            if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory().getHolder() instanceof PlayerInventoryView)
                player.closeInventory();

        playerDataManager.saveAll(false);
    }

    public void reload() {
        reloadConfig();
        language = new ConfigFile("language");
        slotManager.reload();
        debugMode = getConfig().getBoolean("debug");

        try {
            inventorySlots = getConfig().getInt("inventory-slots");
            Validate.isTrue(inventorySlots > 0 && inventorySlots < 55, "Number must be greater than 9 and lower than 54");
            Validate.isTrue(inventorySlots % 9 == 0, "Number must be a multiple of 9");

        } catch (IllegalArgumentException exception) {
            inventorySlots = 36;
            getLogger().log(Level.WARNING, "Invalid inventory slot number: " + exception.getMessage());
        }
    }

    public PlayerDataManager getDataManager() {
        return playerDataManager;
    }

    public SlotManager getSlotManager() {
        return slotManager;
    }

    public LevelModule getLevelModule() {
        return levelModule;
    }

    public ClassModule getClassModule() {
        return classModule;
    }

    public PlaceholderParser getPlaceholderParser() {
        return placeholderParser;
    }

    public void registerInventoryUpdater(InventoryUpdater updater) {
        Validate.notNull(updater, "Updater cannot be null");

        inventoryUpdaters.add(updater);
    }

    /**
     * Iterates through all registered {@link InventoryUpdater}
     * and updates the player for every plugin that needs it.
     *
     * @param player Player which inventory requires an update
     */
    public void updateInventory(Player player) {
        for (InventoryUpdater updater : inventoryUpdaters)
            updater.updateInventory(player);
    }


    public String getTranslation(String path) {
        return ChatColor.translateAlternateColorCodes('&', language.getConfig().getString(path));
    }

    private void saveDefaultFile(String path) {
        try {
            File file = new File(getDataFolder(), path + ".yml");
            if (!file.exists())
                Files.copy(getResource("default/" + path + ".yml"), file.getAbsoluteFile().toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
