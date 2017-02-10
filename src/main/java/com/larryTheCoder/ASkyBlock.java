/*
 * Copyright (C) 2016 larryTheHarry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * IMPOVEMENTS:
 *  - Add /is home [1 - %MAX_HOME%]
 *  - Add Schematic - will took a long time to build
 *  - Player locales - /is locales [String:#]
 * SECURITY FIX:
 *  - Change the player name into UUID
 */
package com.larryTheCoder;

import com.larryTheCoder.task.PluginTask;
import cn.nukkit.Player;
import com.larryTheCoder.utils.Utils;
import com.larryTheCoder.utils.ConfigManager;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.level.generator.biome.Biome;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.scheduler.ServerScheduler;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import com.intellectiualcrafters.updater.Updater;
import com.larryTheCoder.database.Database;
import com.larryTheCoder.database.jdbc.ASConnection;
import com.larryTheCoder.database.jdbc.variables.MySQLDatabase;
import com.larryTheCoder.database.jdbc.variables.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.larryTheCoder.storage.InventorySave;
import com.larryTheCoder.listener.IslandListener;
import com.larryTheCoder.listener.chat.ChatHandler;
import com.larryTheCoder.database.ormlite.ORMLiteDatabase;
import com.larryTheCoder.listener.invitation.InvitationHandler;
import com.larryTheCoder.island.GridManager;
import com.larryTheCoder.island.IslandFallback;
import com.larryTheCoder.island.IslandManager;
import com.larryTheCoder.player.PlayerData;
import com.larryTheCoder.player.TeamManager;
import com.larryTheCoder.player.TeleportLogic;
import com.larryTheCoder.schematic.Schematic;
import com.larryTheCoder.storage.IslandData;
import com.larryTheCoder.utils.Settings;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author larryTheCoder
 */
public class ASkyBlock extends PluginBase implements ASkyBlockAPI {

    public Config cfg;
    public int[] version;
    public ArrayList<String> level = new ArrayList<>();
    public static HashMap<String, Schematic> schematics = new HashMap<>();

    // Managers
    private Config msg;
    private Database db = null;
    private ChatHandler chatHandler;
    private static ASkyBlock object;
    private InvitationHandler invitationHandler;
    private IslandManager manager;
    private GridManager grid;
    private IslandFallback backup;
    private InventorySave inventory;
    private TeamManager managers;
    private TeleportLogic teleportLogic;

    @Override
    public void registerSchematic(File schematic, String name) {
        try {
            ASkyBlock.schematics.put("default", new Schematic(this, schematic));
        } catch (IOException ex) {
            Utils.ConsoleMsg("Unable to add " + schematic.getName() + " Schematic file.");
            if (isDebug()) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public Schematic getSchematic(String key) {
        if (ASkyBlock.schematics.containsKey(key)) {
            return ASkyBlock.schematics.get(key);
        }
        return null;
    }

    public TeleportLogic getTeleportLogic() {
        return teleportLogic;
    }

    /**
     * Get if debugging enabled
     *
     * @return true if enabled
     */
    public boolean isDebug() {
        return ASkyBlock.object.cfg.getBoolean("debug");
    }

    @Override
    public int[] getVersion() {
        return this.version;
    }

    @Override
    public Database getDatabase() {
        return db;
    }

    /**
     * Return of ASkyBlock plug-in
     *
     * @return ASkyBlock
     */
    public static ASkyBlock get() {
        return object;
    }

    @Override
    public String getPluginVersionString() {
        return getDescription().getVersion();
    }

    @Override
    public int[] getPluginVersion() {
        String ver = getDescription().getVersion();
        if (ver.contains("-")) {
            ver = ver.split("-")[0];
        }
        String[] split = ver.split("\\.");
        return new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2])};
    }

    @Override
    public boolean checkVersion(int[] version, int... version2) {
        return version[0] > version2[0] || version[0] == version2[0] && version[1] > version2[1] || version[0] == version2[0]
                && version[1] == version2[1] && version[2] >= version2[2];
    }

    /**
     * Get The ChatHandler instance
     *
     * @api
     * @return ChatHandler
     */
    public ChatHandler getChatHandlers() {
        return chatHandler;
    }

    /**
     * Get The InvitationHandler instance
     *
     * @api
     * @return InvitationHandler
     */
    public InvitationHandler getInvitationHandler() {
        return invitationHandler;
    }

    /**
     * Get the island Manager section
     *
     * @api
     * @return IslandManager
     */
    public IslandManager getIsland() {
        return manager;
    }

    /**
     * Get the GridManager Manager section
     *
     * @api
     * @return GridManager
     */
    public GridManager getGrid() {
        return grid;
    }

    /**
     * Get the GridManager Manager section
     *
     * @api
     * @return InventorySave
     */
    public InventorySave getInventory() {
        return inventory;
    }

    /**
     * Get the Island backup section
     *
     * @api
     * @return GridManager
     */
    public IslandFallback getFallback() {
        return backup;
    }

    /**
     * Get the TeamManager section
     *
     * @api
     * @return BaseEntity
     */
    public TeamManager getTManager() {
        return managers;
    }

    @Override
    public IslandData getIslandInfo(Player player) {
        return getDatabase().getIsland(player.getName(), 1);
    }

    @Override
    public IslandData getIslandInfo(Location location) {
        return getDatabase().getIslandLocation(location.getLevel().getName(), location.getFloorX(), location.getFloorZ());
    }

    @Override
    public PlayerData getPlayerInfo(Player player) {
        return getDatabase().getPlayerData(player);
    }
//  #################################### NON-API ####################################

    @Override
    public void onLoad() {
        if (!(object instanceof ASkyBlock)) {
            object = this;
        }
        // Register generator
        Generator.addGenerator(SkyBlockGenerator.class, "island", SkyBlockGenerator.TYPE_SKYBLOCK);
        // Register TaskManager        
        com.intellectiualcrafters.TaskManager.IMP = new com.intellectiualcrafters.TaskManager();
    }

    @Override
    public void onEnable() {
        initConfig();
        getServer().getLogger().info(getPrefix() + getMsg("onLoad") + getPluginVersionString());
        getServer().getLogger().info(TextFormat.YELLOW + "------------------------------------------------------------");
        initIslands();
        registerObject();
        getServer().getLogger().info(TextFormat.YELLOW + "------------------------------------------------------------");
        getServer().getLogger().info(getPrefix() + getMsg("onEnable"));
    }

    @Override
    public void onDisable() {
        Utils.ConsoleMsg(TextFormat.GREEN + "Saving islands framework");
        saveLevel();
        getDatabase().close();
        Utils.ConsoleMsg(TextFormat.RED + "ASkyBlock ~ Disabled seccessfully");
    }

    /**
     * Load every islands Components
     */
    private void initIslands() {

        PluginManager pm = getServer().getPluginManager();
        chatHandler = new ChatHandler(this);
        this.teleportLogic = new TeleportLogic(this);
        invitationHandler = new InvitationHandler(this);
        getServer().getPluginManager().registerEvents(chatHandler, this);
        pm.registerEvents(new IslandListener(this), this);
        ServerScheduler pd = getServer().getScheduler();
        pd.scheduleRepeatingTask(new PluginTask(this), 1200);
    }

    /**
     * Find all database that had generated
     */
    private void reloadLevel() {
        this.getDatabase().getWorlds().stream().forEach((leveln) -> {
            level.add(leveln);
        });
    }

    /**
     * Reload every level that had generated
     *
     * TO-DO: Support Multi-World
     */
    private void saveLevel() {
        this.getDatabase().saveWorlds(level);
    }

    private void registerObject() {
        loadSchematic();
        generateLevel();
        if (cfg.getBoolean("updater")) {
            Updater.getUpdate();
        }
        manager = new IslandManager(this);
        grid = new GridManager(this);
        managers = new TeamManager(this);
        inventory = new InventorySave(this);
        backup = new IslandFallback(this);
        backup.init();
        this.getServer().getCommandMap().register("SkyBlock", new Commands(this));
        switch (cfg.getString("database.provider").toLowerCase()) {
            case "jdbc":
                if (cfg.getString("database.connection").equalsIgnoreCase("mysql")) {
                    try {
                        this.db = new ASConnection(new MySQLDatabase(cfg.getString("database.MySQL.host"), cfg.getInt("database.MySQL.port"), cfg.getString("database.MySQL.database"), cfg.getString("database.MySQL.username"), cfg.getString("database.MySQL.password")), true);
                    } catch (SQLException | ClassNotFoundException ex) {
                        Utils.ConsoleMsg("Unable to create MySql database");
                    }
                } else {
                    try {
                        this.db = new ASConnection(new SQLiteDatabase(new File(getDataFolder(), cfg.getString("database.SQLite.file-name") + ".db")), true);
                    } catch (SQLException | ClassNotFoundException ex) {
                        Utils.ConsoleMsg("Unable to create Sqlite database");
                    }
                }
                break;
            case "ormlite":
                if (cfg.getString("database.connection").equalsIgnoreCase("mysql")) {
                    this.db = new ORMLiteDatabase(new File(getDataFolder(), cfg.getString("database.SQLite.file-name") + ".db"), true);
                } else {
                    this.db = new ORMLiteDatabase(new File(getDataFolder(), cfg.getString("database.SQLite.file-name") + ".db"));
                }
                break;
            case "yaml":
            case "unknown":
                Utils.ConsoleMsg("&cYAML and Unknown Database not available. Sorry. Using default: JDBC");
                try {
                    this.db = new ASConnection(new SQLiteDatabase(new File(getDataFolder(), cfg.getString("database.SQLite.file-name") + ".db")), true);
                } catch (SQLException | ClassNotFoundException ex) {
                    Utils.ConsoleMsg("Unable to create Sqlite database");
                }
                break;
            default:
                Utils.ConsoleMsg("&c" + cfg.getString("database.provider") + " is not available. Sorry. Using default: JDBC");
                try {
                    this.db = new ASConnection(new SQLiteDatabase(new File(getDataFolder(), cfg.getString("database.SQLite.file-name") + ".db")), true);
                } catch (SQLException | ClassNotFoundException ex) {
                    Utils.ConsoleMsg("&cUnable to create Sqlite database");
                }
                break;
        }
        reloadLevel();
        Level world = getServer().getLevelByName("SkyBlock");
        world.setTime(1600);
        world.stopTime();
    }

    public String getPrefix() {
        return cfg.getString("Prefix").replace("&", "§");
    }

    public String getMsg(String key) {
        String mssg = msg.getString(key).replace("&", "§");
        return mssg;
    }

    public final void initConfig() {
        Utils.EnsureDirectory(Utils.DIRECTORY);
        Utils.EnsureDirectory(Utils.LOCALES_DIRECTORY);
        //initLocales();        
        if (this.getResource("config.yml") != null) {
            this.saveResource("config.yml");
        }
        cfg = new Config(new File(getDataFolder(), "config.yml"), Config.YAML);
        if (this.getResource("English.yml") != null) {
            this.saveResource("English.yml");
        }
        msg = new Config(new File(getDataFolder(), "English.yml"), Config.YAML);
        ConfigManager.load();
    }

    /**
     * List schematics this player can access. If @param ignoreNoPermission is
     * true, then only schematics with a specific permission set will be
     * checked. I.e., no common schematics will be returned (including the
     * default one).
     *
     * @param player
     * @param ignoreNoPermission
     * @return List of schematics this player can use based on their permission
     * level
     */
    public List<Schematic> getSchematics(Player player, boolean ignoreNoPermission) {
        List<Schematic> result = new ArrayList<>();
        // Find out what schematics this player can choose from
        //Bukkit.getLogger().info("DEBUG: Checking schematics for " + player.getName());
        schematics.values().stream().filter((schematic) -> ((!ignoreNoPermission && schematic.getPerm().isEmpty()) || player.hasPermission(schematic.getPerm()))).filter((schematic) -> (schematic.isVisible())).forEach((schematic) -> {
            // Check if it's a nether island, but the nether is not enables
            if (schematic.getBiome().equals(Biome.HELL)) {

            } else {
                result.add(schematic);
            }
        }); //Bukkit.getLogger().info("DEBUG: schematic name is '"+ schematic.getName() + "'");
        //Bukkit.getLogger().info("DEBUG: perm is " + schematic.getPerm());
        //Bukkit.getLogger().info("DEBUG: player can use this schematic");
        // Only add if it's visible
        // Sort according to order
        Collections.sort(result, (Schematic o1, Schematic o2) -> ((o2.getOrder() < o1.getOrder()) ? 1 : -1));
        return result;
    }

    private void generateLevel() {
        Utils.ConsoleMsg(TextFormat.GREEN + "Loading the Island Framework");
        if (!Server.getInstance().isLevelGenerated("SkyBlock")) {
            Server.getInstance().generateLevel("SkyBlock", 0, SkyBlockGenerator.class);
        }
        if (!Server.getInstance().isLevelLoaded("SkyBlock")) {
            Server.getInstance().loadLevel("SkyBlock");
        }
        level.stream().forEach((world) -> {
            getServer().loadLevel(world);
        });
        level.add("SkyBlock");
    }

    private void loadSchematic() {
        // Check if there is a schematic folder and make it if it does not exist
        File schematicFolder = new File(getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdir();
        }
        getServer().getLogger().info("§7<§cSMC§7> §eSchematic Post:");
        // Clear the schematic list that is kept in memory
        schematics.clear();
        // Load the default schematic if it exists
        // Set up the default schematic
        File schematicFile;
        // This part loads the schematic section
        ConfigSection schemSection = cfg.getSection("schematicsection");
        Settings.useSchematicPanel = schemSection.getBoolean("useschematicspanel", false);
        Settings.chooseIslandRandomly = schemSection.getBoolean("chooseislandrandomly", true);
        for (String key : schemSection.getSection("schematics").getKeys(false)) {
            try {
                Schematic newSchem = null;
                // Check the file exists
                String filename = schemSection.getString("schematics." + key + ".filename", "");
                if (!filename.isEmpty()) {
                    // Check if this file exists or if it is in the jar
                    schematicFile = new File(schematicFolder, filename);
                    // See if the file exists
                    if (schematicFile.exists()) {
                        newSchem = new Schematic(this, schematicFile);
                    } else if (getResource("schematics/" + filename) != null) {
                        saveResource("schematics/" + filename, false);
                        newSchem = new Schematic(this, schematicFile);
                    }
                } else if (key.equalsIgnoreCase("default")) {
                    newSchem = schematics.get("default");
                } else {
                    getServer().getLogger().info(TextFormat.YELLOW + "   - Schematic " + key + " does not have a filename. Skipping!");
                }
                if (newSchem != null) {
                    // Set the heading
                    //newSchem.setHeading(key);
                    // Order
                    newSchem.setOrder(schemSection.getInt("schematics." + key + ".order", 0));
                    // Load the rest of the settings
                    // Icon
                    try {

                        Item icon;
                        String iconString = schemSection.getString("schematics." + key + ".icon", "MAP").toUpperCase();
                        // Support damage values
                        String[] split = iconString.split(":");
                        if (Utils.isNumeric(split[0])) {
                            icon = Item.get(Integer.parseInt(iconString));
                        } else {
                            icon = Item.fromString(split[0]);
                        }
                        int damage = 0;
                        if (split.length == 2) {
                            if (Utils.isNumeric(split[1])) {
                                damage = Integer.parseInt(split[1]);
                            }
                        }
                        newSchem.setIcon(icon, damage);
                    } catch (Exception e) {
                        newSchem.setIcon(Item.MAP);
                    }
                    // Friendly name
                    String name = TextFormat.colorize('&', schemSection.getString("schematics." + key + ".name", ""));
                    newSchem.setName(name);
                    // Rating - Rating is not used right now
                    int rating = schemSection.getInt("schematics." + key + ".rating", 50);
                    if (rating < 1) {
                        rating = 1;
                    } else if (rating > 100) {
                        rating = 100;
                    }
                    newSchem.setRating(rating);
                    // Description
                    String description = TextFormat.colorize('&', schemSection.getString("schematics." + key + ".description", ""));
                    description = description.replace("[rating]", String.valueOf(rating));
                    newSchem.setDescription(description);
                    // Permission
                    String perm = schemSection.getString("schematics." + key + ".permission", "");
                    newSchem.setPerm(perm);
                    // Use default chest
                    newSchem.setUseDefaultChest(schemSection.getBoolean("schematics." + key + ".useDefaultChest", true));
                    // Biomes - overrides default if it exists
                    String biomeString = schemSection.getString("schematics." + key + ".biome", Settings.defaultBiome.toString());
                    Biome biome = null;
                    try {
                        biome = Biome.getBiome(biomeString);
                        newSchem.setBiome(biome);
                    } catch (Exception e) {
                        Utils.ConsoleMsg("Could not parse biome " + biomeString + " using default instead.");
                    }
                    // Use physics - overrides default if it exists
                    //newSchem.setUsePhysics(schemSection.getBoolean("schematics." + key + ".usephysics", Settings.usePhysics));
                    // Paste Entities or not
                    newSchem.setPasteEntities(schemSection.getBoolean("schematics." + key + ".pasteentities", false));
                    // Paste air or not. Default is false - huge performance savings!
                    //newSchem.setPasteAir(schemSection.getBoolean("schematics." + key + ".pasteair",false));	    
                    // Visible in GUI or not
                    newSchem.setVisible(schemSection.getBoolean("schematics." + key + ".show", true));
                    // Partner schematic
                    if (biome != null && biome.equals(Biome.HELL)) {
                        // Default for nether biomes is the default overworld island
                        newSchem.setPartnerName(schemSection.getString("schematics." + key + ".partnerSchematic", "default"));
                    } else {
                        // Default for overworld biomes is nether island
                        newSchem.setPartnerName(schemSection.getString("schematics." + key + ".partnerSchematic", "nether"));
                    }
                    // Get chest items
                    final List<String> chestItems = schemSection.getStringList("schematics." + key + ".chestItems");
                    if (!chestItems.isEmpty()) {
                        Item[] tempChest = new Item[chestItems.size()];
                        int i = 0;
                        for (String chestItemString : chestItems) {
                            try {
                                String[] amountdata = chestItemString.split(":");
                                Item mat;
                                if (Utils.isNumeric(amountdata[0])) {
                                    mat = Item.get(Integer.parseInt(amountdata[0]));
                                } else {
                                    mat = Item.fromString(amountdata[0].toUpperCase());
                                }
                                if (amountdata.length == 2) {
                                    tempChest[i] = new Item(mat.getId(), Integer.parseInt(amountdata[1]));
                                    i++;
                                } else if (amountdata.length == 3) {
                                    tempChest[i] = new Item(mat.getId(), Integer.parseInt(amountdata[2]), Short.parseShort(amountdata[1]));
                                    i++;
                                }

                            } catch (Exception e) {
                                getServer().getLogger().info(TextFormat.YELLOW + "   - Problem loading chest item for schematic " + name + " so skipping it: " + chestItemString);
                            }
                        }

                        // Store it
                        newSchem.setDefaultChestItems(tempChest);
                    }
                    // Player spawn block
                    String spawnBlock = schemSection.getString("schematics." + key + ".spawnblock");
                    if (!spawnBlock.isEmpty()) {
                        // Check to see if this block is a valid material
                        try {
                            Item playerSpawnBlock;
                            if (Utils.isNumeric(spawnBlock)) {
                                playerSpawnBlock = Item.get(Integer.parseInt(spawnBlock));
                            } else {
                                playerSpawnBlock = Item.fromString(spawnBlock.toUpperCase());
                            }
                            if (newSchem.setPlayerSpawnBlock(Block.get(playerSpawnBlock.getId()))) {
                                getServer().getLogger().info(TextFormat.YELLOW + "   - Player will spawn at the " + playerSpawnBlock.toString());
                            } else {
                                getServer().getLogger().info(TextFormat.YELLOW + "   - Problem with schematic '" + name + "'. Spawn block '" + spawnBlock + "' not found in schematic or there is more than one. Skipping...");
                            }
                        } catch (Exception e) {
                            getServer().getLogger().info(TextFormat.YELLOW + "   - Problem with schematic '" + name + "'. Spawn block '" + spawnBlock + "' is unknown. Skipping...");
                        }
                    } else {
                        // plugin.getLogger().info("No spawn block found");
                    }
                    // Level handicap
                    newSchem.setLevelHandicap(schemSection.getInt("schematics." + key + ".levelHandicap", 0));

                    // Store it
                    schematics.put(key, newSchem);
                }
            } catch (Exception ex) {
                getServer().getLogger().info(TextFormat.YELLOW + "   - Error loading schematic in section " + key + ". Skipping...");
            }
        }
        // Try to load schematic the default schematic
        if (schematics.isEmpty()) {
            // Load the default schematic if it exists
            // Set up the default schematic
            schematicFile = new File(schematicFolder, "island.schematic");
            File netherFile = new File(schematicFolder, "nether.schematic");
            getServer().getLogger().info(TextFormat.YELLOW + " - " + schematicFile.getName().toUpperCase().replace(".SCHEMATIC", "") + " Info:");
            if (!schematicFile.exists()) {
                //plugin.getLogger().info("Default schematic does not exist...");
                // Only copy if the default exists
                if (getResource("schematics/island.schematic") != null) {
                    getServer().getLogger().info("§aDefault schematic does not exist, saving it...");
                    saveResource("schematics/island.schematic", false);
                    // Add it to schematics
                    try {
                        schematics.put("default", new Schematic(this, schematicFile));
                    } catch (IOException e) {
                        getServer().getLogger().info("Could not load default schematic!");
                        if (ASkyBlock.get().isDebug()) {
                            e.printStackTrace();
                        }
                    }
                    // If this is repeated later due to the schematic config, fine, it will only add info
                } else {
                    // No islands.schematic in the jar, so just make the default using 
                    // built-in island generation
                    schematics.put("default", new Schematic(this));
                }
            } else {
                // It exists, so load it
                try {
                    schematics.put("default", new Schematic(this, schematicFile));
                } catch (IOException e) {
                    getServer().getLogger().error("Could not load default schematic!");
                    if (ASkyBlock.get().isDebug()) {
                        e.printStackTrace();
                    }
                }
            }
            getServer().getLogger().info(TextFormat.YELLOW + " - " + netherFile.getName().toUpperCase().replace(".SCHEMATIC", "") + " Info:");
            // Add the nether default too
            if (!netherFile.exists()) {
                if (getResource("schematics/nether.schematic") != null) {
                    saveResource("schematics/nether.schematic", false);
                    // Add it to schematics
                    try {
                        Schematic netherIsland = new Schematic(this, netherFile);
                        //netherIsland.setVisible(false);
                        schematics.put("nether", netherIsland);
                    } catch (IOException e) {
                        getServer().getLogger().error("Could not load default nether schematic!");
                        if (ASkyBlock.get().isDebug()) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    getServer().getLogger().error("Could not find default nether schematic!");
                }
            } else {
                // It exists, so load it
                try {
                    Schematic netherIsland = new Schematic(this, netherFile);
                    //netherIsland.setVisible(false);
                    schematics.put("nether", netherIsland);
                } catch (IOException e) {
                    getServer().getLogger().error("Could not load default nether schematic!");
                    if (ASkyBlock.get().isDebug()) {
                        e.printStackTrace();
                    }
                }
            }
            // Set up some basic settings just in case the schematics section is missing
            if (schematics.containsKey("default")) {
                schematics.get("default").setName("Island");
                schematics.get("default").setDescription("");
                schematics.get("default").setPartnerName("nether");
                schematics.get("default").setBiome(Settings.defaultBiome);
                schematics.get("default").setIcon(Item.GRASS);
                if (Settings.chestItems.length == 0) {
                    schematics.get("default").setUseDefaultChest(false);
                }
                schematics.get("default").setOrder(0);
            }
            if (schematics.containsKey("nether")) {
                schematics.get("nether").setName("NetherBlock Island");
                schematics.get("nether").setDescription("Nether Island");
                schematics.get("nether").setPartnerName("default");
                schematics.get("nether").setBiome(Biome.HELL);
                schematics.get("nether").setIcon(Item.NETHERRACK);
                schematics.get("nether").setVisible(false);
                if (Settings.chestItems.length == 0) {
                    schematics.get("nether").setUseDefaultChest(false);
                }
            }
        }
        getServer().getLogger().info("§7<§cSMC§7> §aSeccessfully loaded island Schematic");

    }
}