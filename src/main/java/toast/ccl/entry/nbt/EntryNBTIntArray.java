package toast.ccl.entry.nbt;

import net.minecraft.nbt.NBTTagIntArray;
import toast.ccl.ChestLootException;
import toast.ccl.FileHelper;
import toast.ccl.IPropertyReader;
import toast.ccl.entry.EntryAbstract;
import toast.ccl.entry.NBTStatsInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class EntryNBTIntArray extends EntryAbstract {
    // The name of this tag.
    private final String name;
    // The values of this tag.
    private final double[][] values;

    public EntryNBTIntArray(String path, JsonObject root, int index, JsonObject node, IPropertyReader loader) {
        super(node, path);
        this.name = FileHelper.readText(node, path, "name", "");

        JsonArray nodes = node.getAsJsonArray("value");
        if (nodes == null)
            throw new ChestLootException("Missing or invalid value!", path);

        int length = nodes.size();
        this.values = new double[length][];
        for (int i = 0; i < length; i++) {
            this.values[i] = FileHelper.readCounts(node, path, "value", i, 0.0, 0.0);
        }
    }

    // Returns an array of required field names.
    @Override
    public String[] getRequiredFields() {
        return new String[] { };
    }

    // Returns an array of optional field names.
    @Override
    public String[] getOptionalFields() {
        return new String[] { "name", "value" };
    }

    // Adds any NBT tags to the list.
    @Override
    public void addTags(NBTStatsInfo nbtStats) {
        int[] value = new int[this.values.length];
        for (int i = value.length; i-- > 0;) {
            value[i] = FileHelper.getCount(this.values[i], nbtStats.random);
        }
        nbtStats.addTag(this.name, new NBTTagIntArray(value));
    }
}