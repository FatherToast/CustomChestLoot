package toast.ccl;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import toast.ccl.entry.PropertyExternal;

import com.google.gson.JsonObject;

/**
 * Used to add new, more complex, generation options.
 */
public class CustomChestContent extends WeightedRandomChestContent
{
	public static CustomChestContent readContent(String path, JsonObject root, int index, JsonObject node) {
		// Skip verification, should be done before calling this

        int weight = FileHelper.readInteger(node, path, "weight", 1);

		Item item = FileHelper.readItem(node, path, "id", false);
		double[] damages = FileHelper.readCounts(node, path, "damage", 0, 0);
		int min = Math.max(0, FileHelper.readInteger(node, path, "min", 1));
		int max = Math.max(min, FileHelper.readInteger(node, path, "max", min));

		double[] enchantLevels = FileHelper.readCounts(node, path, "enchant", 5.0, 30.0);
		double[] enchantChances = FileHelper.readCounts(node, path, "enchant_chance", 0.0, 0.0);

		String file = FileHelper.readText(node, path, "external", null);
		IProperty external;
		if (file == null) {
			external = null;
		}
		else {
			external = new PropertyExternal(path, file);
		}
		ItemStats functions = new ItemStats(path, root, index, node, "functions", external);

		boolean hasItem;
		if (item == null) {
			item = Items.cookie; // Replace with dummy item
			hasItem = false;
		}
		else {
			hasItem = true;
		}

		return new CustomChestContent(weight, hasItem, item, damages, min, max, enchantLevels, enchantChances, functions);
	}

	private final boolean hasItem;

	private final Item item;
	private final double[] damages;
	private final int min, max;

	private final double[] enchantLevels;
	private final double[] enchantChances;

	private final ItemStats functions;

	private CustomChestContent(int weight, boolean hasItem, Item item, double[] damages, int min, int max, double[] enchantLevels, double[] enchantChances, ItemStats functions) {
		super(item, (int) damages[0], min, max, weight);

		this.hasItem = hasItem;
		this.item = item;
		this.damages = damages;
		this.min = min;
		this.max = max;

		this.enchantLevels = enchantLevels;
		this.enchantChances = enchantChances;

		this.functions = functions;
	}

	// Called to generate an array of item stacks to be put in the inventory.
	public void generateChestContent(ArrayList<ItemStack> items, Random random, IInventory newInventory) {
        int damage = FileHelper.getCount(this.damages, random);
        int count = this.min + random.nextInt(this.max - this.min + 1);

        // Make the item to place in the chest
        ItemStack dropStack = this.functions == null ? new ItemStack(this.item, 1, damage) : this.functions.generate(items, random, newInventory, this.item, damage, this);
    	if (random.nextDouble() < FileHelper.getValue(this.enchantChances, random)) {
    		try {
    			EnchantmentHelper.addRandomEnchantment(random, dropStack, FileHelper.getCount(this.enchantLevels, random));
    		}
    		catch (Exception ex) {
    			_CustomChestLootMod.logWarning("Failed to enchant item! (" + dropStack.toString() + ")", ex);
    		}
    	}

    	// Place the item in the list with an appropriate stack size
    	if (this.hasItem) {
	        ItemStack drop;
	        while (count > 0) {
	            drop = dropStack.copy();
	            drop.stackSize = Math.min(newInventory.getInventoryStackLimit(), count);
	            count -= drop.stackSize;
	            items.add(drop);
	        }
    	}
    }
	// Called to generate an array of item stacks to be put in the inventory.
    @Override
	protected ItemStack[] generateChestContent(Random random, IInventory newInventory) {
    	ArrayList<ItemStack> items = new ArrayList<ItemStack>();
    	this.generateChestContent(items, random, newInventory);
        return items.toArray(new ItemStack[0]);
    }
}
