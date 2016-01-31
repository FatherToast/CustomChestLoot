package toast.ccl.entry;

import java.util.HashMap;

import toast.ccl.ChestLootException;
import toast.ccl.FileHelper;
import toast.ccl.IProperty;
import toast.ccl.IPropertyReader;
import toast.ccl.ItemStats;
import toast.ccl.NBTStats;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PropertyExternal implements IProperty {
    // Mapping of all loaded external functions to their file name.
    private static final HashMap<String, ItemStats> ITEMS_MAP = new HashMap<String, ItemStats>();
    private static final HashMap<String, NBTStats> NBT_MAP = new HashMap<String, NBTStats>();

    // Unloads all properties.
    public static void unload() {
        PropertyExternal.ITEMS_MAP.clear();
        PropertyExternal.NBT_MAP.clear();
    }

    // Turns a string of info into data. Crashes the game if something goes wrong.
    public static void load(String type, String path, String fileName, JsonObject node) {
        String name = fileName.substring(0, fileName.length() - 5);
        if (type.equals("items")) {
            PropertyExternal.loadItem(path, name, node);
        }
        else if (type.equals("nbt")) {
            PropertyExternal.loadNbt(path, name, node);
        }
    }
    private static void loadItem(String path, String name, JsonObject node) {
        if (PropertyExternal.ITEMS_MAP.containsKey(name))
            throw new ChestLootException("Duplicate external item stats property! (name: " + name + ")", path);

        JsonObject dummyRoot = new JsonObject();
        JsonArray dummyArray = new JsonArray();
        dummyArray.add(node);
        dummyRoot.add("item_stats", dummyArray);
        PropertyExternal.ITEMS_MAP.put(name, new ItemStats(path, dummyRoot, 0, dummyRoot, "item_stats", null));
    }
    private static void loadNbt(String path, String name, JsonObject node) {
        if (PropertyExternal.NBT_MAP.containsKey(name))
            throw new ChestLootException("Duplicate external nbt stats property! (name: " + name + ")", path);

        JsonObject dummyRoot = new JsonObject();
        JsonArray dummyArray = new JsonArray();
        dummyArray.add(node);
        dummyRoot.add("tags", dummyArray);
        PropertyExternal.NBT_MAP.put(name, new NBTStats(path, dummyRoot, 0, dummyRoot, null));
    }

    // The min and max number of times to perform the task.
    private final double[] counts;
    // The name of the external function to use.
    private final String externalFunction;

    public PropertyExternal(String path, String file) {
        this.counts = new double[] { 0.0, 0.0 };

        this.externalFunction = file;
        if (this.externalFunction == "")
            throw new ChestLootException("Missing or invalid external file name!", path);
    }
    public PropertyExternal(String path, JsonObject root, int index, JsonObject node, IPropertyReader loader) {
        FileHelper.verify(node, path, this);
        this.counts = FileHelper.readCounts(node, path, "count", 1.0, 1.0);

        this.externalFunction = FileHelper.readText(node, path, "file", "");
        if (this.externalFunction == "")
            throw new ChestLootException("Missing or invalid external file name!", path);
    }

    // Returns an array of required field names.
    @Override
    public String[] getRequiredFields() {
        return new String[] { "file" };
    }

    // Returns an array of optional field names.
    @Override
    public String[] getOptionalFields() {
        return new String[] { "count" };
    }

    // Modifies the item.
    @Override
    public void modifyItem(ItemStatsInfo itemStats) {
        ItemStats stats = PropertyExternal.ITEMS_MAP.get(this.externalFunction);
        if (stats != null) {
            for (int count = FileHelper.getCount(this.counts); count-- > 0;) {
                stats.generate(itemStats);
            }
        }
        else
			throw new UnsupportedOperationException("Non-item properties can not modify items!");
    }

    // Adds any NBT tags to the list.
    @Override
    public void addTags(NBTStatsInfo nbtStats) {
        NBTStats stats = PropertyExternal.NBT_MAP.get(this.externalFunction);
        if (stats != null) {
            for (int count = FileHelper.getCount(this.counts); count-- > 0;) {
                stats.generate(nbtStats);
            }
        }
        else
			throw new UnsupportedOperationException("Non-nbt properties can not modify nbt!");
    }
}
