package toast.ccl.entry.nbt;

import net.minecraft.nbt.NBTTagInt;
import toast.ccl.FileHelper;
import toast.ccl.IPropertyReader;
import toast.ccl.entry.NBTStatsInfo;

import com.google.gson.JsonObject;

public class EntryNBTInt extends EntryNBTNumber {
    public EntryNBTInt(String path, JsonObject root, int index, JsonObject node, IPropertyReader loader) {
        super(path, root, index, node, loader);
    }

    // Adds any NBT tags to the list.
    @Override
    public void addTags(NBTStatsInfo nbtStats) {
        int value = FileHelper.getCount(this.values, nbtStats.random);
        nbtStats.addTag(this.name, new NBTTagInt(value));
    }
}