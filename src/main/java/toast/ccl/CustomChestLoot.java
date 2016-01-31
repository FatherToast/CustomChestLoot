package toast.ccl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;
import toast.ccl.entry.item.EntryItemLoot;

import com.google.common.collect.HashMultiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 *
 */
public class CustomChestLoot
{
    /** A semi-deep copy of ChestGenHooks's original loot mappings. */
    private static final HashMap<String, ChestGenHooks> ORIGINAL_CHEST_INFO = new HashMap<String, ChestGenHooks>();
	private static boolean LOADED_ORIGINALS = false;

	public static final HashSet<String> LOADED_CATEGORIES = new HashSet<String>();
    private static Field CONTENTS_FIELD;

	public static HashMap<String, ChestGenHooks> CHEST_INFO;

    public static ArrayList<WeightedRandomChestContent> getContents(ChestGenHooks chestGen) {
		try {
			return (ArrayList<WeightedRandomChestContent>) CustomChestLoot.CONTENTS_FIELD.get(chestGen);
    	}
    	catch (Exception ex) {
    		_CustomChestLootMod.logError("Failed to find chest contents field!", ex);
    		return null;
		}
    }
    public static void setContents(ChestGenHooks chestGen, ArrayList<WeightedRandomChestContent> contents) {
		try {
			CustomChestLoot.CONTENTS_FIELD.set(chestGen, contents);
    	}
    	catch (Exception ex) {
    		_CustomChestLootMod.logError("Failed to find chest contents field!", ex);
		}
    }

	/** Resets ChestGenHooks to its original settings, or loads the originals if needed. */
	public static void resetLoot() {
		if (CustomChestLoot.LOADED_ORIGINALS) {
			CustomChestLoot.copyChestInfo(CustomChestLoot.ORIGINAL_CHEST_INFO, CustomChestLoot.CHEST_INFO);
		}
		else {
			CustomChestLoot.copyChestInfo(CustomChestLoot.CHEST_INFO, CustomChestLoot.ORIGINAL_CHEST_INFO);
			CustomChestLoot.LOADED_ORIGINALS = true;
		}
		CustomChestLoot.LOADED_CATEGORIES.clear();
	}
	private static void copyChestInfo(HashMap<String, ChestGenHooks> copyFrom, HashMap<String, ChestGenHooks> copyTo) {
		copyTo.clear();
		ChestGenHooks copy;
		for (Map.Entry<String, ChestGenHooks> original : copyFrom.entrySet()) {
			copy = new ChestGenHooks(original.getKey());
			copy.setMin(original.getValue().getMin());
			copy.setMax(original.getValue().getMax());
			CustomChestLoot.setContents(copy, (ArrayList<WeightedRandomChestContent>) CustomChestLoot.getContents(original.getValue()).clone());

			copyTo.put(original.getKey(), copy);
		}
	}

	public static void load(String path, JsonObject node) {
		FileHelper.verify(node, path, new String[] { "_name" }, new String[] { "min", "max", "loot" });

		String name = FileHelper.readText(node, path, "_name", "");
		if (CustomChestLoot.LOADED_CATEGORIES.contains(name)) // Results are undefined if two loot files have the same name, so we prevent that
			throw new ChestLootException("Duplicate loot list! (name: " + name + ")", path);
		CustomChestLoot.LOADED_CATEGORIES.add(name);
		ChestGenHooks lootList = ChestGenHooks.getInfo(name);

		lootList.setMin(Math.max(0, FileHelper.readInteger(node, path, "min", lootList.getMin())));
		lootList.setMax(Math.max(lootList.getMin(), FileHelper.readInteger(node, path, "max", lootList.getMax())));

		if (node.has("loot")) {
			JsonArray loot = node.getAsJsonArray("loot");
	        path += "\\loot";
	        // Build list of items to add and keep
	        ArrayList<WeightedRandomChestContent> newItems = new ArrayList<WeightedRandomChestContent>();
	        HashMultiset<RetainedLoot> keepItems = HashMultiset.create();
	        int length = loot.size();
	        for (int i = 0; i < length; i++) {
	            CustomChestLoot.readLootItem(path, node, i, loot.get(i), newItems, keepItems);
	        }
	        // Remove all non-kept items
	        ArrayList<WeightedRandomChestContent> items = CustomChestLoot.getContents(lootList);
	        WeightedRandomChestContent item;
	        for (Iterator<WeightedRandomChestContent> iterator = items.iterator(); iterator.hasNext();) {
	        	item = iterator.next();
	        	if (item.getClass() == WeightedRandomChestContent.class || !keepItems.remove(new RetainedLoot(item))) {
        			iterator.remove();
	        	}
	        }
	        if (!keepItems.isEmpty()) {
	        	_CustomChestLootMod.logWarning("Unused \"keep\" entries! " + keepItems.toString());
	        }
	        // Add new items
	        items.addAll(newItems);
		}
	}

	// The new loot list becomes the list of new items plus the originals kept by the list of default items to keep.
	private static void readLootItem(String path, JsonObject root, int index, JsonElement node, ArrayList<WeightedRandomChestContent> newItems, HashMultiset<RetainedLoot> keepItems) {
        path += "\\entry_" + (index + 1);
        if (!node.isJsonObject())
            throw new ChestLootException("Invalid node (object expected)!", path);
        JsonObject objNode = node.getAsJsonObject();

        boolean retainedLoot;
        try {
        	// Try reading as retained loot
    		FileHelper.verify(objNode, path, new String[] { "keep" }, new String[] { "id", "damage", "nbt", "count" });
    		retainedLoot = true;
        }
        catch (Exception ex) {
        	// It is not retained loot, continue as a loot item
    		retainedLoot = false;
        }
        if (retainedLoot) {
            path += "(keep)";
        	Class lootClass;
        	try {
        		lootClass = Class.forName(FileHelper.readText(objNode, path, "keep", ""));
        	}
	        catch (Exception ex) {
	        	throw new ChestLootException("Invalid class for \"keep\"! (See \"Caused by:\" below for more info.)", path, ex);
	        }
        	Item item = FileHelper.readItem(objNode, path, "id", false);
        	int damage = FileHelper.readInteger(objNode, path, "damage", 0);
        	boolean hasNbt = FileHelper.readBoolean(objNode, path, "nbt", false);

			keepItems.add(new RetainedLoot(lootClass, item, damage, hasNbt), FileHelper.readInteger(objNode, path, "count", 1));
			return;
        }

        path += "(id)";
        // Quickly get weight, min, and max, if available (always allowed fields for loot items)
        int weight = FileHelper.readInteger(objNode, path, "weight", 1);
		int min = Math.max(0, FileHelper.readInteger(objNode, path, "min", 1));
		int max = Math.max(min, FileHelper.readInteger(objNode, path, "max", min));

    	try {
    		// Try reading as a simple loot item
    		FileHelper.verify(objNode, path, new String[] { "id" }, new String[] { "weight", "damage", "min", "max" });
    		int damage = FileHelper.readInteger(objNode, path, "damage", 0);
    		Item item = FileHelper.readItem(objNode, path, "id", true);
    		// No errors, therefore it is a simple loot item
    		newItems.add(new WeightedRandomChestContent(item, damage, min, max, weight));
    		return;
    	}
        catch (Exception ex) {
        	// It is not a simple loot item, continue to read as advanced
        }

		FileHelper.verify(objNode, path, EntryItemLoot.REQUIRED_FIELDS, EntryItemLoot.OPTIONAL_FIELDS);
		newItems.add(CustomChestContent.readContent(path, root, index, objNode));
	}

	// Writes information to the provided node that is equivalent to the loot item.
	public static JsonObject duplicateLootItem(WeightedRandomChestContent lootItem) {
		JsonObject node = new JsonObject();

		if (lootItem.getClass() != WeightedRandomChestContent.class) {
			// Cannot duplicate exactly, so use retain loot
			node.addProperty("keep", lootItem.getClass().getName());

			if (lootItem.theItemId != null) {
				node.addProperty("id", Item.itemRegistry.getNameForObject(lootItem.theItemId.getItem()));
				node.addProperty("damage", lootItem.theItemId.getItemDamage());
				node.addProperty("nbt", lootItem.theItemId.stackTagCompound != null);
				if (lootItem.theItemId.stackTagCompound != null) {
					node.addProperty("_comment", "Nbt tag: " + lootItem.theItemId.stackTagCompound.toString());
				}
			}
			node.addProperty("count", 1);
			return node;
		}

		node.addProperty("weight", lootItem.itemWeight);
		if (lootItem.theItemId != null) {
			node.addProperty("id", Item.itemRegistry.getNameForObject(lootItem.theItemId.getItem()));
			node.addProperty("damage", lootItem.theItemId.getItemDamage());

			node.addProperty("min", lootItem.theMinimumChanceToGenerateItem);
			node.addProperty("max", lootItem.theMaximumChanceToGenerateItem);

			node.addProperty("damage", lootItem.theItemId.getItemDamage());

			if (lootItem.theItemId.stackTagCompound != null) {
				// Needs to be replaced with an "advanced" loot item
				node.addProperty("enchant", 0);
				node.addProperty("enchant_chance", 0);

				JsonObject nbtFunc = new JsonObject();
				nbtFunc.addProperty("function", "nbt");
				nbtFunc.add("tags", FileHelper.tagsToJsonArray(lootItem.theItemId.stackTagCompound));
				JsonArray functions = new JsonArray();
				functions.add(nbtFunc);
				node.add("functions", functions);
			}
		}
		else {
			node.addProperty("_comment", "[WARNING] Auto-generated entry for null item");
		}
		return node;
	}

    static {
    	try {
			Field chestInfo = ChestGenHooks.class.getDeclaredField("chestInfo");
			chestInfo.setAccessible(true);
			CustomChestLoot.CHEST_INFO = (HashMap<String, ChestGenHooks>) chestInfo.get(null);
		}
    	catch (Exception ex) {
    		_CustomChestLootMod.logError("Failed to find chest info field!", ex);
		}
    	try {
    		CustomChestLoot.CONTENTS_FIELD = ChestGenHooks.class.getDeclaredField("contents");
    		CustomChestLoot.CONTENTS_FIELD.setAccessible(true);
    	}
    	catch (Exception ex) {
    		_CustomChestLootMod.logError("Failed to find chest contents field!", ex);
		}
    }

    private static class RetainedLoot {
    	private static final NBTTagCompound emptyTag = new NBTTagCompound();

    	private final Class<? extends WeightedRandomChestContent> lootClass;
    	private final ItemStack matchItem;

    	public RetainedLoot(WeightedRandomChestContent loot) {
    		this.lootClass = loot.getClass();
    		this.matchItem = loot.theItemId.copy();
    		this.matchItem.stackSize = 1;
    		this.matchItem.stackTagCompound = this.matchItem.stackTagCompound != null ? RetainedLoot.emptyTag : null;
    	}
    	public RetainedLoot(Class<? extends WeightedRandomChestContent> lootClass, Item item, int damage, boolean hasNbt) {
    		this.lootClass = lootClass;
    		this.matchItem = new ItemStack(item, 1, damage);
    		this.matchItem.stackTagCompound = hasNbt ? RetainedLoot.emptyTag : null;
    	}

    	@Override
		public String toString() {
    		return this.lootClass.getName() + ":" + (this.matchItem == null ? "undefinedItem" : this.matchItem.toString() + "{" + (this.matchItem.stackTagCompound == null ? "noNbt" : "hasNbt") + "}");
    	}

    	@Override
		public boolean equals(Object obj) {
    		if (obj instanceof RetainedLoot) {
    			RetainedLoot other = (RetainedLoot) obj;
    			return this.lootClass == other.lootClass &&
    					(this.matchItem == null && other.matchItem == null || // Both items are null
    					// OR
    					this.matchItem.getItem() == other.matchItem.getItem() && // Same item id
    					this.matchItem.getItemDamage() == other.matchItem.getItemDamage() && // Same item damage
    					this.matchItem.stackTagCompound == null == (other.matchItem.stackTagCompound == null)); // Both have or both don't have nbt data
    		}
    		return false;
    	}
    	@Override
		public int hashCode() {
    		return this.lootClass.hashCode() ^ (this.matchItem == null ? 0 : this.matchItem.getItem().hashCode() ^ this.matchItem.getItemDamage() ^ (this.matchItem.stackTagCompound == null ? 0 : ~0));
    	}
    }
}
