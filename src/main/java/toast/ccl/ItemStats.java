package toast.ccl;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import toast.ccl.entry.ItemStatsInfo;
import toast.ccl.entry.PropertyChoose;
import toast.ccl.entry.PropertyExternal;
import toast.ccl.entry.PropertyGroup;
import toast.ccl.entry.PropertyGroupConditional;
import toast.ccl.entry.item.EntryItemColor;
import toast.ccl.entry.item.EntryItemEnchant;
import toast.ccl.entry.item.EntryItemLoot;
import toast.ccl.entry.item.EntryItemLore;
import toast.ccl.entry.item.EntryItemModifier;
import toast.ccl.entry.item.EntryItemNBT;
import toast.ccl.entry.item.EntryItemName;
import toast.ccl.entry.item.EntryItemPotion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ItemStats implements IPropertyReader {
    // The entry objects included in this property.
    public final IProperty[] entries;

    public ItemStats(String path, JsonObject root, int index, JsonObject node, String tag, IProperty extra) {
        JsonArray nodes = node.getAsJsonArray(tag);
        if (nodes == null) {
            this.entries = extra == null ? new IProperty[0] : new IProperty[] { extra };
        }
        else {
        	int extraOffset = extra == null ? 0 : 1;
            int length = nodes.size();
        	this.entries = new IProperty[length + extraOffset];
            if (extra != null) {
            	this.entries[0] = extra;
            }
            for (int i = 0; i < length; i++) {
                this.entries[i + extraOffset] = this.readLine(path, root, i, nodes.get(i));
            }
        }
    }

    // Generates an appropriate item stack with a stack size of 1.
    public ItemStack generate(ArrayList<ItemStack> items, Random random, IInventory inventory, Item item, int damage, Object mobInfo) {
        ItemStack itemStack = new ItemStack(item, 1, damage);
        ItemStatsInfo info = new ItemStatsInfo(itemStack, items, random, inventory, mobInfo);
        for (IProperty entry : this.entries) {
            if (entry != null) {
                entry.modifyItem(info);
            }
        }
        return itemStack;
    }
    public void generate(ItemStatsInfo info) {
        for (IProperty entry : this.entries) {
            if (entry != null) {
                entry.modifyItem(info);
            }
        }
    }

    // Loads a line as a mob property.
    @Override
    public IProperty readLine(String path, JsonObject root, int index, JsonElement node) {
        path += "\\entry_" + (index + 1);
        if (!node.isJsonObject())
            throw new ChestLootException("Invalid node (object expected)!", path);
        JsonObject objNode = node.getAsJsonObject();
        String function = null;
        try {
            function = objNode.get("function").getAsString();
        }
        catch (NullPointerException ex) {
            // Do nothing
        }
        catch (IllegalArgumentException ex) {
            // Do nothing
        }
        if (function == null)
            throw new ChestLootException("Missing function name!", path);
        path += "(" + function + ")";

        if (function.equals("all"))
            return new PropertyGroup(path, root, index, objNode, this);
        if (function.equals("choose"))
            return new PropertyChoose(path, root, index, objNode, this);
        if (function.equals("external"))
            return new PropertyExternal(path, root, index, objNode, this);

        if (function.equals("loot"))
            return new EntryItemLoot(path, root, index, objNode, this);
        if (function.equals("name"))
            return new EntryItemName(path, root, index, objNode, this);
        if (function.equals("modifier"))
            return new EntryItemModifier(path, root, index, objNode, this);
        if (function.equals("potion"))
            return new EntryItemPotion(path, root, index, objNode, this);
        if (function.equals("nbt"))
            return new EntryItemNBT(path, root, index, objNode, this);
        if (function.equals("enchant"))
            return new EntryItemEnchant(path, root, index, objNode, this);
        if (function.equals("lore"))
            return new EntryItemLore(path, root, index, objNode, this);
        if (function.equals("color"))
            return new EntryItemColor(path, root, index, objNode, this);

        boolean inverted = false;
        if (function.startsWith(Character.toString(FileHelper.CHAR_INVERT))) {
            inverted = true;
            function = function.substring(1);
        }
        if (function.startsWith("if_"))
            return new PropertyGroupConditional(path, root, index, objNode, this, function.substring(3), inverted);
        throw new ChestLootException("Invalid function name!", path);
    }
}
