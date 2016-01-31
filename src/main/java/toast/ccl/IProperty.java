package toast.ccl;

import toast.ccl.entry.ItemStatsInfo;
import toast.ccl.entry.NBTStatsInfo;

public interface IProperty
{
    // Returns an array of required field names.
    public String[] getRequiredFields();

    // Returns an array of optional field names.
    public String[] getOptionalFields();

    // Modifies the item.
    public void modifyItem(ItemStatsInfo itemStats);

    // Adds any NBT tags to the list.
    public void addTags(NBTStatsInfo nbtStats);
}
