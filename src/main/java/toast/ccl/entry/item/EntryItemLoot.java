package toast.ccl.entry.item;

import toast.ccl.CustomChestContent;
import toast.ccl.IPropertyReader;
import toast.ccl.entry.EntryAbstract;
import toast.ccl.entry.ItemStatsInfo;

import com.google.gson.JsonObject;

public class EntryItemLoot extends EntryAbstract
{
	public static final String[] REQUIRED_FIELDS = { };
	public static final String[] OPTIONAL_FIELDS = { "weight", "id", "damage", "min", "max", "enchant", "enchant_chance", "external", "functions" };

	private final CustomChestContent lootItem;

    public EntryItemLoot(String path, JsonObject root, int index, JsonObject node, IPropertyReader loader) {
        super(node, path);
		this.lootItem = CustomChestContent.readContent(path, root, index, node);
    }

    // Returns an array of required field names.
    @Override
    public String[] getRequiredFields() {
        return EntryItemLoot.REQUIRED_FIELDS;
    }

    // Returns an array of optional field names.
    @Override
    public String[] getOptionalFields() {
        return EntryItemLoot.OPTIONAL_FIELDS;
    }

    // Modifies the item.
    @Override
    public void modifyItem(ItemStatsInfo itemStats) {
        this.lootItem.generateChestContent(itemStats.itemList, itemStats.random, itemStats.inventory);
    }
}