package toast.ccl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import toast.ccl.entry.PropertyExternal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public abstract class FileHelper
{
    // The directory for config files.
    public static File CONFIG_DIRECTORY;
    // The main directory for custom chest loot.
    public static File PROPS_DIRECTORY;
    // The directory for external functions.
    public static File EXTERNAL_DIRECTORY;
    // The directory for files generated with /cclinfo.
    public static File INFO_DIRECTORY;

    // The file extention for mob properties files.
    public static final String FILE_EXT = ".json";
    // The file extention for schematic files.
    public static final String SCHEMATIC_FILE_EXT = ".schematic";
    // The json parser object for reading json.
    private static final JsonParser PARSER = new JsonParser();
    // The gson objects for writing json.
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Gson GSON_COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    // Special characters recognized by the property reader.
    public static final char CHAR_RAND = '~';
    public static final char CHAR_INVERT = '!';

	public static Gson getGsonFormatter() {
		return FileHelper.GSON_PRETTY;
	}

    // Initializes this file helper.
    public static void init(File directory) {
        FileHelper.CONFIG_DIRECTORY = directory;
        FileHelper.PROPS_DIRECTORY = new File(directory, "CustomChestLoot");
        FileHelper.PROPS_DIRECTORY.mkdirs();
        FileHelper.EXTERNAL_DIRECTORY = new File(directory, "CustomChestLootExternal");
        FileHelper.EXTERNAL_DIRECTORY.mkdirs();
        FileHelper.INFO_DIRECTORY = new File(directory, "CustomChestLootInfo");
        // Do not mkdirs the info directory
    }

    // Recursively turns an nbt tag compound into an array of Json function objects.
    public static JsonArray tagsToJsonArray(NBTTagCompound compound) {
    	JsonArray array = new JsonArray();
        for (String name : (Collection<String>) compound.func_150296_c()) { // Gets all used names
        	array.add(FileHelper.tagToJson(name, compound.getTag(name), compound));
        }
        return array;
    }
    // Recursively turns an nbt tag list into an array of Json function objects.
    private static JsonArray tagsToJsonArray(NBTTagList list) {
    	// Makes a temp copy to avoid clearing the original, as now the only
    	// way to get a list's NBTBase objects is by removing them
    	NBTTagList listCopy = (NBTTagList) list.copy();
    	JsonArray array = new JsonArray();
        while (listCopy.tagCount() > 0) {
        	array.add(FileHelper.tagToJson(null, listCopy.removeTag(0), null));
        }
        return array;
    }
    // Returns a single nbt tag as a Json object. (Parent is often null.)
    private static JsonObject tagToJson(String name, NBTBase tag, NBTTagCompound parent) {
        JsonObject json = new JsonObject();
        if (name != null) {
        	json.addProperty("name", name);
        }

        Class tagClass = tag.getClass();
        // Nbt objects
        if (NBTTagCompound.class.equals(tagClass)) {
        	json.addProperty("function", "compound");
        	json.add("tags", FileHelper.tagsToJsonArray((NBTTagCompound) tag));
        }
        else if (NBTTagList.class.equals(tagClass)) {
        	json.addProperty("function", "list");
        	json.add("tags", FileHelper.tagsToJsonArray((NBTTagList) tag));
        }
        // Nbt primitives
        else if (NBTTagString.class.equals(tagClass)) {
        	json.addProperty("function", "string");
        	json.addProperty("value", ((NBTTagString) tag).func_150285_a_());
        }
        else if (NBTTagByte.class.equals(tagClass)) {
	        // Check if it is potentially a potion id
			try {
	            if (parent != null && "Id".equals(name)) {
	            	Collection<String> parentNames = parent.func_150296_c();
	            	if (parentNames.size() == 4 && parentNames.contains("Amplifier") && parentNames.contains("Duration") && parentNames.contains("Ambient")) {
	    	        	json.addProperty("function", "potion_id");
	    	        	String value = Potion.potionTypes[((NBTTagByte) tag).func_150290_f()].getName();
	    	        	json.addProperty("value", value);
	            		return json;
	            	}
	            }
			}
			catch (Exception ex) {
				// Standard takes precedence in case of error
			}

			// Standard byte
        	json.addProperty("function", "byte");
        	String value = Byte.toString(((NBTTagByte) tag).func_150290_f());
        	json.addProperty("value", value + "~" + value);
        }
        else if (NBTTagByteArray.class.equals(tagClass)) {
        	json.addProperty("function", "byte_array");
        	JsonArray value = new JsonArray();
        	String subVal;
        	for (byte entry : ((NBTTagByteArray) tag).func_150292_c()) {
        		subVal = Byte.toString(entry);
        		value.add(new JsonPrimitive(subVal + "~" + subVal));
        	}
        	json.add("value", value);
        }
        else if (NBTTagShort.class.equals(tagClass)) {
            // Check if it is potentially an item or enchant id
    		try {
	            if (parent != null && "id".equals(name)) {
	            	Collection<String> parentNames = parent.func_150296_c();
	            	if (parentNames.size() == 2 && parentNames.contains("lvl")) {
	    	        	json.addProperty("function", "enchant_id");
	    	        	String value = Enchantment.enchantmentsList[((NBTTagShort) tag).func_150289_e()].getName();
	    	        	json.addProperty("value", value);
	            		return json;
	            	}
	            	else if (parentNames.contains("Count") && parentNames.contains("Damage")) {
	    	        	json.addProperty("function", "item_id");
	    	        	String value = Item.itemRegistry.getNameForObject(Item.getItemById(((NBTTagShort) tag).func_150289_e()));
	    	        	json.addProperty("value", value);
	            		return json;
	            	}
	            }
    		}
    		catch (Exception ex) {
    			// Standard takes precedence in case of error
    		}

    		// Standard short
        	json.addProperty("function", "short");
        	String value = Short.toString(((NBTTagShort) tag).func_150289_e());
        	json.addProperty("value", value + "~" + value);
        }
        else if (NBTTagInt.class.equals(tagClass)) {
        	json.addProperty("function", "int");
        	String value = Integer.toString(((NBTTagInt) tag).func_150287_d());
        	json.addProperty("value", value + "~" + value);
        }
        else if (NBTTagIntArray.class.equals(tagClass)) {
        	json.addProperty("function", "int_array");
        	JsonArray value = new JsonArray();
        	String subVal;
        	for (int entry : ((NBTTagIntArray) tag).func_150302_c()) {
        		subVal = Integer.toString(entry);
        		value.add(new JsonPrimitive(subVal + "~" + subVal));
        	}
        	json.add("value", value);
        }
        else if (NBTTagLong.class.equals(tagClass)) {
        	json.addProperty("function", "long");
        	String value = Long.toString(((NBTTagLong) tag).func_150291_c());
        	json.addProperty("value", value);
        }
        else if (NBTTagFloat.class.equals(tagClass)) {
        	json.addProperty("function", "float");
        	String value = Float.toString(((NBTTagFloat) tag).func_150288_h());
        	json.addProperty("value", value + "~" + value);
        }
        else if (NBTTagDouble.class.equals(tagClass)) {
        	json.addProperty("function", "double");
        	String value = Double.toString(((NBTTagDouble) tag).func_150286_g());
        	json.addProperty("value", value + "~" + value);
        }
        // Special cases will have already returned
        return json;
    }

    // Loads the chest gen properties, creates defaults.
    public static int load() {
        CustomChestLoot.resetLoot();

        int filesLoaded = 0;
        String[] types = { "items", "nbt" };
        File externalDir;
        for (String type : types) {
            externalDir = new File(FileHelper.EXTERNAL_DIRECTORY, type);
            externalDir.mkdirs();
            filesLoaded += FileHelper.loadExternalDirectory(type, externalDir);
        }
        filesLoaded += FileHelper.loadDirectory(FileHelper.PROPS_DIRECTORY);
        return filesLoaded;
    }

    // Recursively loads the loot lists in the given directory.
    private static int loadDirectory(File directory) {
        int filesLoaded = 0;
        JsonObject node;
        for (File propFile : directory.listFiles(new ExtensionFilter(FileHelper.FILE_EXT))) {
            node = FileHelper.loadFile(propFile);
            CustomChestLoot.load(propFile.getPath(), node);
            filesLoaded++;
        }
        for (File subDirectory : directory.listFiles(new FolderFilter())) {
            filesLoaded += FileHelper.loadDirectory(subDirectory);
        }
        return filesLoaded;
    }

    // Recursively loads the external functions in the given directory.
    private static int loadExternalDirectory(String type, File directory) {
        int filesLoaded = 0;
        JsonObject node;
        for (File propFile : directory.listFiles(new ExtensionFilter(FileHelper.FILE_EXT))) {
            node = FileHelper.loadFile(propFile);
            PropertyExternal.load(type, propFile.getPath(), propFile.getName(), node);
            filesLoaded++;
        }
        for (File subDirectory : directory.listFiles(new FolderFilter())) {
            filesLoaded += FileHelper.loadExternalDirectory(type, subDirectory);
        }
        return filesLoaded;
    }

    // Generates a trivial property file for each loot list, if a file does not already exist.
    public static int generateDefaults() {
        int filesGenerated = 0;

        String fileName;
        File propFile;
        JsonObject root;
        JsonArray loot;
		for (Map.Entry<String, ChestGenHooks> entry : CustomChestLoot.CHEST_INFO.entrySet()) {
			if (!CustomChestLoot.LOADED_CATEGORIES.contains(entry.getKey())) {
                char[] fileNameArray = entry.getKey().toCharArray();
                fileName = "";
                for (char letter : fileNameArray) {
                    fileName += Character.isLetterOrDigit(letter) ? Character.toString(letter) : "_";
                }

                try {
                    propFile = new File(FileHelper.PROPS_DIRECTORY, fileName + FileHelper.FILE_EXT);
                    if (propFile.exists()) {
                        int attempt = 0;
                        for (; attempt < 100; attempt++)
                            if (! (propFile = new File(FileHelper.PROPS_DIRECTORY, fileName + attempt + FileHelper.FILE_EXT)).exists()) {
                                break;
                            }
                        if (attempt >= 100) {
                            _CustomChestLootMod.logWarning("Failed to generate default properties file for \"" + entry.getKey() + "\"!");
                            continue;
                        }
                        fileName += attempt;
                    }
					root = new JsonObject();

					root.addProperty("_name", entry.getKey());
					root.addProperty("min", entry.getValue().getMin());
					root.addProperty("max", entry.getValue().getMax());

					loot = new JsonArray();
					ArrayList<WeightedRandomChestContent> items = CustomChestLoot.getContents(entry.getValue());
					for (WeightedRandomChestContent item : items) {
						loot.add(CustomChestLoot.duplicateLootItem(item));
					}
					root.add("loot", loot);

                    propFile.createNewFile();
                    FileWriter out = new FileWriter(propFile);
                    out.write(FileHelper.getGsonFormatter().toJson(root).replace("\u00a7", "\\u00a7"));
                    out.close();
                    filesGenerated++;
                }
                catch (ChestLootException ex) {
                    throw ex;
                }
                catch (Exception ex) {
                    _CustomChestLootMod.logWarning("Failed to generate default properties file for \"" + entry.getKey() + "\"!");
                    ex.printStackTrace();
                }
			}
		}

        return filesGenerated;
    }

    // Creates a file containing all default chest loot tables.
    public static String toEntryString(ArrayList<WeightedRandomChestContent> contents) {
        boolean first = true;
        String line = "";
        for (WeightedRandomChestContent item : contents) {
            if (first) {
                first = false;
            }
            else {
                line += ",";
            }
            line += Item.itemRegistry.getNameForObject(item.theItemId.getItem()) + ";" + Integer.toString(item.theItemId.getItemDamage()) + ";" + Integer.toString(item.theMinimumChanceToGenerateItem) + "-" + Integer.toString(item.theMaximumChanceToGenerateItem) + ";" + Integer.toString(item.itemWeight);
        }
        return line;
    }

    // Loads a file as a Json node object. Throws an exception if it fails.
    private static JsonObject loadFile(File propFile) {
        JsonElement node = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(propFile)));
            node = FileHelper.PARSER.parse(in);
            in.close();
        }
        catch (Exception ex) {
            throw new ChestLootException("Error reading file! (See \"Caused by:\" below for more info.)", propFile.getPath(), ex);
        }
        if (node == null)
            throw new ChestLootException("Failed to read file", propFile.getPath());
        if (!node.isJsonObject())
            throw new ChestLootException("Invalid file! (non-object)", propFile.getPath());
        return node.getAsJsonObject();
    }

    // Makes sure that only defined fields are being used.
    public static void verify(JsonObject node, String path, String[] requiredFields, String[] optionalFields) {
        List<String> required = Arrays.asList(requiredFields);
        List<String> optional = Arrays.asList(optionalFields);
        HashSet<String> allowed = new HashSet<String>();
        allowed.addAll(required);
        allowed.addAll(optional);
        allowed.add("_comment");

        try {
            Set<Map.Entry<String, JsonElement>> fields = node.entrySet();
            HashSet<String> fieldNames = new HashSet<String>();
            for (Map.Entry<String, JsonElement> entry : fields) {
                fieldNames.add(entry.getKey());
            }

            for (String name : required)
                if (!fieldNames.contains(name))
                    throw new ChestLootException("Verify error! Missing required field \"" + name + "\". (Required fields: " + Arrays.toString(requiredFields) + ")", path);
            for (String name : fieldNames)
                if (!allowed.contains(name))
                    throw new ChestLootException("Verify error! Invalid field \"" + name + "\". (Allowed fields: " + Arrays.toString(allowed.toArray(new String[0])) + ")", path);
        }
        catch (IllegalStateException ex) {
            throw new ChestLootException("Verify error! (must be an object)", path);
        }
    }

    // Makes sure that only defined fields are being used.
    public static void verify(JsonObject node, String path, IProperty property) {
        List<String> required = Arrays.asList(property.getRequiredFields());
        List<String> optional = Arrays.asList(property.getOptionalFields());
        HashSet<String> allowed = new HashSet<String>();
        allowed.addAll(required);
        allowed.addAll(optional);
        allowed.add("_comment");
        allowed.add("function");
        if (path.matches("^.*\\\\entry_[0-9]+\\(choose\\)\\\\functions\\\\entry_[0-9]+\\(\\w+\\)$")) {
            allowed.add("weight");
        }

        try {
            Set<Map.Entry<String, JsonElement>> fields = node.entrySet();
            HashSet<String> fieldNames = new HashSet<String>();
            for (Map.Entry<String, JsonElement> entry : fields) {
                fieldNames.add(entry.getKey());
            }

            for (String name : required)
                if (!fieldNames.contains(name))
                    throw new ChestLootException("Verify error! Missing required field \"" + name + "\". (Required fields: " + Arrays.toString(property.getRequiredFields()) + ")", path);
            for (String name : fieldNames)
                if (!allowed.contains(name))
                    throw new ChestLootException("Verify error! Invalid field \"" + name + "\". (Allowed fields: " + Arrays.toString(allowed.toArray(new String[0])) + ")", path);
        }
        catch (IllegalStateException ex) {
            throw new ChestLootException("Verify error! (functions must be objects)", path);
        }
    }

    // Returns a function as a compact string.
    public static String getFunctionString(JsonObject node, String path) {
        try {
            return FileHelper.GSON_COMPACT.toJson(node);
        }
        catch (Exception ex) {
            throw new ChestLootException("Error generating function string!", path, ex);
        }
    }

    // Loads a function from the given string.
    public static JsonObject loadFunctionFromString(String path, String prop, int index) {
        try {
            path += "\\entry_" + (index + 1);
            JsonObject node = null;
            try {
                node = FileHelper.PARSER.parse(prop).getAsJsonObject();
            }
            catch (Exception ex) {
                throw new ChestLootException("Error loading function string! " + prop, path, ex);
            }
            if (node == null)
                throw new ChestLootException("Failed to load function string! " + prop, path);
            if (!node.isJsonObject())
                throw new ChestLootException("Invalid function string! (non-object) " + prop, path);

            return node;
        }
        catch (ChestLootException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Returns a randomized double within the values' range.
    public static double getValue(double[] values) {
        return FileHelper.getCount(values, _CustomChestLootMod.random);
    }
    public static double getValue(double[] values, Random random) {
        if (values[0] == values[1])
            return values[0];
        return random.nextDouble() * (values[1] - values[0]) + values[0];
    }

    // Returns a randomized integer within the counts' range.
    public static int getCount(double[] counts) {
        return FileHelper.getCount(counts, _CustomChestLootMod.random);
    }
    public static int getCount(double[] counts, Random random) {
        double count = FileHelper.getValue(counts, random);
        int intCount = (int) Math.floor(count);
        count -= intCount;
        if (0.0 < count && random.nextDouble() < count) {
            intCount++;
        }
        return intCount;
    }

    // Returns a randomized long within the counts' range.
    public static long getLongCount(double[] counts) {
        return FileHelper.getLongCount(counts, _CustomChestLootMod.random);
    }
    public static long getLongCount(double[] counts, Random random) {
        double count = FileHelper.getValue(counts, random);
        long longCount = (long) Math.floor(count);
        count -= longCount;
        if (0.0 < count && random.nextDouble() < count) {
            longCount++;
        }
        return longCount;
    }

    // Returns the text of the node, or the default.
    public static String readText(JsonObject node, String path, String tag, String defaultValue) {
        try {
            return node.get(tag).getAsString();
        }
        catch (NullPointerException ex) { // The object does not exist.
            return defaultValue;
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid value for \"" + tag + "\"! (wrong node type)", path);
        }
    }

    // Returns the boolean value of the node, or the default.
    public static boolean readBoolean(JsonObject node, String path, String tag, boolean defaultValue) {
        String text = FileHelper.readText(node, path, tag, Boolean.toString(defaultValue));
        if (text.equals("false"))
            return false;
        else if (text.equals("true"))
            return true;
        throw new ChestLootException("Invalid boolean value! (" + text + ": must be true or false)", path);
    }

    // Reads the line part as a number range.
    public static double[] readCounts(JsonObject node, String path, String tag, int index, double defaultMin, double defaultMax) {
        path += "\\" + tag + "\\entry_" + (index + 1);
        String value = "";
        try {
            value = node.getAsJsonArray(tag).get(index).getAsString();
        }
        catch (NullPointerException ex) { // The object does not exist.
            return new double[] { defaultMin, defaultMax };
        }
        catch (IndexOutOfBoundsException ex) {
            throw new ChestLootException("Unexpected error! (array index out of bounds)", path);
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid number range! (wrong node type)", path);
        }
        return FileHelper.readCounts(value, path);
    }
    public static double[] readCounts(JsonObject node, String path, String tag, double defaultMin, double defaultMax) {
        path += "\\" + tag;
        String value = "";
        try {
            value = node.get(tag).getAsString();
        }
        catch (NullPointerException ex) { // The object does not exist.
            return new double[] { defaultMin, defaultMax };
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid number range! (wrong node type)", path);
        }
        return FileHelper.readCounts(value, path);
    }
    private static double[] readCounts(String value, String path) {
        double[] counts = new double[2];
        String[] subParts = value.split(Character.toString(FileHelper.CHAR_RAND));
        try {
            if (subParts[0].startsWith("0x")) {
                counts[0] = Integer.parseInt(subParts[0].substring(2), 16);
            }
            else {
                counts[0] = Double.valueOf(subParts[0]).doubleValue();
            }
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid number range! (" + subParts[0].trim() + ")", path);
        }
        if (subParts.length == 1) {
            counts[1] = counts[0];
        }
        else if (subParts.length == 2) {
            try {
                if (subParts[1].startsWith("0x")) {
                    counts[1] = Integer.parseInt(subParts[1].substring(2), 16);
                }
                else {
                    counts[1] = Double.valueOf(subParts[1]).doubleValue();
                }
            }
            catch (Exception ex) {
                throw new ChestLootException("Invalid number range! (" + subParts[1].trim() + ")", path);
            }
        }
        else
            throw new ChestLootException("Invalid number range! (too many \'~\'s)", path);
        if (Double.isNaN(counts[0]) || Double.isNaN(counts[1]) || Double.isInfinite(counts[0]) || Double.isInfinite(counts[1]))
            throw new ChestLootException("Invalid number range! (NaN and Infinity are not allowed)", path);
        if (counts[0] > counts[1]) {
            double temp = counts[0];
            counts[0] = counts[1];
            counts[1] = temp;
        }
        return counts;
    }

    // Reads the object's weight.
    public static int readWeight(JsonObject node, String path, int defaultValue) {
        String value = "";
        try {
            value = node.get("weight").getAsString();
        }
        catch (NullPointerException ex) { // The object does not exist.
            return defaultValue;
        }
        catch (IndexOutOfBoundsException ex) {
            throw new ChestLootException("Unexpected error! (array index out of bounds)", path);
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid number range! (wrong node type)", path);
        }

        try {
            int weight = Integer.parseInt(value);
            if (weight < 0)
                throw new ChestLootException("Invalid weight! (" + value + ": must be non-negative)", path);
            return weight;
        }
        catch (NumberFormatException ex) {
            throw new ChestLootException("Invalid weight! (" + value + ")", path, ex);
        }
    }

    // Reads the line part as an integer.
    public static int readInteger(JsonObject node, String path, String tag, int defaultValue) {
        path += "\\" + tag;
        String value = "";
        try {
            value = node.get(tag).getAsString();
        }
        catch (NullPointerException ex) {
            return defaultValue;
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid integer! (wrong node type)", path);
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            throw new ChestLootException("Invalid integer! (" + value + ")", path, ex);
        }
    }

    // Reads the line part as a double.
    public static double readDouble(JsonObject node, String path, String tag) {
        return FileHelper.readDouble(node, path, tag, Double.NaN);
    }
    public static double readDouble(JsonObject node, String path, String tag, double defaultValue) {
        path += "\\" + tag;
        String value = "";
        try {
            value = node.get(tag).getAsString();
        }
        catch (NullPointerException ex) {
            return defaultValue;
        }
        catch (Exception ex) {
            throw new ChestLootException("Invalid double! (wrong node type)", path);
        }

        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ex) {
            throw new ChestLootException("Invalid double! (" + value + ")", path, ex);
        }
    }

    // Reads the line and throws an exception if it does not represent a valid item.
    public static Item readItem(JsonObject node, String path, String tag) {
        return FileHelper.readItem(node, path, tag, true);
    }

    // Reads the line and optionally throws an exception if it does not represent a valid item.
    public static Item readItem(JsonObject node, String path, String tag, boolean required) {
        return FileHelper.readItem(FileHelper.readText(node, path, tag, ""), path, required);
    }

    // Reads the text and optionally throws an exception if it does not represent a valid item.
    public static Item readItem(String id, String path, boolean required) {
        Item item = (Item) Item.itemRegistry.getObject(id);

        // Compatibility with old numerical ids.
        if (item == null) {
            try {
                item = Item.getItemById(Integer.parseInt(id));
                if (item != null) {
                    _CustomChestLootMod.logWarning("Usage of numerical item id! (" + id + "=\"" + Item.itemRegistry.getNameForObject(item) + "\") at " + path);
                }
            }
            catch (NumberFormatException ex) {
                // Do nothing
            }
        }

        if (required && item == null)
            throw new ChestLootException("Missing or invalid item!", path);
        return item;
    }

    // Reads the line and throws an exception if it does not represent a valid block.
    public static Block readBlock(JsonObject node, String path, String tag) {
        return FileHelper.readBlock(node, path, tag, true);
    }

    // Reads the line and optionally throws an exception if it does not represent a valid block.
    public static Block readBlock(JsonObject node, String path, String tag, boolean required) {
        return FileHelper.readBlock(FileHelper.readText(node, path, tag, ""), path, required);
    }

    // Reads the text and optionally throws an exception if it does not represent a valid block.
    public static Block readBlock(String id, String path, boolean required) {
    	if (id.equals("air") || id.equals("minecraft:air"))
    		return Blocks.air;

        Block block = Block.getBlockFromName(id);
        if (block == null || block == Blocks.air) {
            try {
                block = Block.getBlockById(Integer.parseInt(id));
                if (block != null && block != Blocks.air) {
                    _CustomChestLootMod.logWarning("Usage of numerical block id! (" + id + "=\"" + Block.blockRegistry.getNameForObject(block) + "\") at " + path);
                }
            }
            catch (NumberFormatException ex) {
                // Do nothing
            }
        }
        if (required && (block == null || block == Blocks.air))
            throw new ChestLootException("Missing or invalid block!", path);
        return block;
    }

    // Reads the line and throws an exception if it does not represent a valid potion.
    public static Potion readPotion(JsonObject node, String path, String tag) {
        return FileHelper.readPotion(node, path, tag, true);
    }
    // Reads the line and optionally throws an exception if it does not represent a valid potion.
    public static Potion readPotion(JsonObject node, String path, String tag, boolean required) {
        return FileHelper.readPotion(FileHelper.readText(node, path, tag, ""), path, required);
    }
    // Reads the text and optionally throws an exception if it does not represent a valid potion.
    public static Potion readPotion(String id, String path, boolean required) {
        Potion potion = null;
    	for (Potion potionType : Potion.potionTypes) {
    		if (potionType != null && id.equals(potionType.getName())) {
    			potion = potionType;
    			break;
    		}
    	}
    	if (potion == null) {
    		try {
    			potion = Potion.potionTypes[Integer.parseInt(id)];
                if (potion != null) {
                    _CustomChestLootMod.logWarning("Usage of numerical potion id! (" + id + "=\"" + potion.getName() + "\") at " + path);
                }
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                // Do nothing
            }
            catch (NumberFormatException ex) {
                // Do nothing
            }
    	}
        if (required && potion == null)
			throw new ChestLootException("Missing or invalid potion! (" + id + ")", path);
        return potion;
    }

    // Reads the line and throws an exception if it does not represent a valid enchantment.
    public static Enchantment readEnchant(JsonObject node, String path, String tag) {
        return FileHelper.readEnchant(node, path, tag, true);
    }
    // Reads the line and optionally throws an exception if it does not represent a valid enchantment.
    public static Enchantment readEnchant(JsonObject node, String path, String tag, boolean required) {
        return FileHelper.readEnchant(FileHelper.readText(node, path, tag, ""), path, required);
    }
    // Reads the text and optionally throws an exception if it does not represent a valid enchantment.
    public static Enchantment readEnchant(String id, String path, boolean required) {
    	Enchantment enchant = null;
    	for (Enchantment enchantType : Enchantment.enchantmentsList) {
    		if (enchantType != null && id.equals(enchantType.getName())) {
    			enchant = enchantType;
    			break;
    		}
    	}
    	if (enchant == null) {
    		try {
    			enchant = Enchantment.enchantmentsList[Integer.parseInt(id)];
                if (enchant != null) {
                    _CustomChestLootMod.logWarning("Usage of numerical enchantment id! (" + id + "=\"" + enchant.getName() + "\") at " + path);
                }
            }
            catch (ArrayIndexOutOfBoundsException ex) {
                // Do nothing
            }
            catch (NumberFormatException ex) {
                // Do nothing
            }
    	}
        if (required && enchant == null)
			throw new ChestLootException("Missing or invalid enchantment! (" + id + ")", path);
        return enchant;
    }

	// All the file filters used.
    public static class ExtensionFilter implements FilenameFilter {
        // The file extension to accept.
        private final String extension;

        public ExtensionFilter(String ext) {
            this.extension = ext;
        }

        // Returns true if the file should be accepted.
        @Override
        public boolean accept(File file, String name) {
            return name.toLowerCase().endsWith(this.extension);
        }
    }

    public static class FolderFilter implements FileFilter {
        public FolderFilter() {
        }

        // Returns true if the file should be accepted.
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }
}
