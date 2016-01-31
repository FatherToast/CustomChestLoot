package toast.ccl;

import java.util.Random;

import net.minecraft.command.ServerCommandManager;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = _CustomChestLootMod.MODID, name = "Custom Chest Loot", version = _CustomChestLootMod.VERSION)
public class _CustomChestLootMod {
    /* TO DO *\
    \* ** ** */
    // This mod's id.
    public static final String MODID = "CustomChestLoot";
    // This mod's version.
    public static final String VERSION = "2.0.0";

    // If true, this mod starts up in debug mode.
    public static final boolean debug = false;
    // The mod's random number generator.
    public static final Random random = new Random();

    // Called before initialization. Loads the properties/configurations.
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        _CustomChestLootMod.logDebug("Loading in debug mode!");
        Properties.init(new Configuration(event.getSuggestedConfigurationFile()));
        FileHelper.init(event.getModConfigurationDirectory());
    }

    // Called after initialization. Used to check for dependencies.
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        _CustomChestLootMod.log("Loading custom chest loot...");
        _CustomChestLootMod.log("Loaded " + FileHelper.load() + " loot lists!");
        if (Properties.getBoolean(Properties.GENERAL, "auto_generate_files")) {
            _CustomChestLootMod.log("Generating default chest loot...");
            _CustomChestLootMod.log("Generated " + FileHelper.generateDefaults() + " loot lists!");
        }
    }

    // Called as the server is starting.
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        ServerCommandManager commandManager = (ServerCommandManager) event.getServer().getCommandManager();
        commandManager.registerCommand(new CommandReload());
        commandManager.registerCommand(new CommandInfo());
        commandManager.registerCommand(new CommandTest());
    }

    // Makes the first letter upper case.
    public static String cap(String string) {
        int length = string.length();
        if (length <= 0)
            return "";
        if (length == 1)
            return string.toUpperCase();
        return Character.toString(Character.toUpperCase(string.charAt(0))) + string.substring(1);
    }

    // Makes the first letter lower case.
    public static String decap(String string) {
        int length = string.length();
        if (length <= 0)
            return "";
        if (length == 1)
            return string.toLowerCase();
        return Character.toString(Character.toLowerCase(string.charAt(0))) + string.substring(1);
    }

    /** Prints the message to the console with this mod's name tag. */
    public static void log(String message) {
        System.out.println("[" + _CustomChestLootMod.MODID + "] " + message);
    }
    /** Prints the message to the console with this mod's name tag if debugging is enabled. */
    public static void logDebug(String message) {
        if (_CustomChestLootMod.debug) {
            System.out.println("[" + _CustomChestLootMod.MODID + "] [debug] " + message);
        }
    }
    /** Prints the message to the console with this mod's name tag and a warning tag. */
    public static void logWarning(String message) {
        System.out.println("[" + _CustomChestLootMod.MODID + "] [WARNING] " + message);
    }
    /** Prints the message to the console with this mod's name tag and a warning tag. */
    public static void logWarning(String message, Exception ex) {
        System.out.println("[" + _CustomChestLootMod.MODID + "] [WARNING] " + message);
        ex.printStackTrace();
    }
    /** Prints the message to the console with this mod's name tag and an error tag.<br>
     * Throws a runtime exception with a message and this mod's name tag if debugging is enabled. */
    public static void logError(String message) {
        if (_CustomChestLootMod.debug)
            throw new RuntimeException("[" + _CustomChestLootMod.MODID + "] " + message);
        _CustomChestLootMod.log("[ERROR] " + message);
    }
    /** Prints the message to the console with this mod's name tag and an error tag.<br>
     * Throws a runtime exception with a message and this mod's name tag if debugging is enabled. */
    public static void logError(String message, Exception ex) {
        if (_CustomChestLootMod.debug)
            throw new RuntimeException("[" + _CustomChestLootMod.MODID + "] " + message, ex);
        _CustomChestLootMod.log("[ERROR] " + message);
        ex.printStackTrace();
    }
    /** Throws a runtime exception with a message and this mod's name tag. */
    public static void exception(String message) {
        throw new RuntimeException("[" + _CustomChestLootMod.MODID + "] " + message);
    }
    /** Throws a runtime exception with a message and this mod's name tag. */
    public static void exception(String message, Exception ex) {
        throw new RuntimeException("[" + _CustomChestLootMod.MODID + "] " + message, ex);
    }
}
