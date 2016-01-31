package toast.ccl.entry;

import toast.ccl.FileHelper;
import toast.ccl.IProperty;

import com.google.gson.JsonObject;

public abstract class EntryAbstract implements IProperty {
    // The Json string that makes up this property.
    private final String jsonString;

    public EntryAbstract(JsonObject node, String path) {
        FileHelper.verify(node, path, this);
        this.jsonString = FileHelper.getFunctionString(node, path);
    }

    // Returns this property's Json string.
    public String getJsonString() {
        return this.jsonString;
    }

    // Modifies the item.
    @Override
    public void modifyItem(ItemStatsInfo itemStats) {
        throw new UnsupportedOperationException("Non-item properties can not modify items!");
    }

    // Adds any NBT tags to the list.
    @Override
    public void addTags(NBTStatsInfo nbtStats) {
        throw new UnsupportedOperationException("Non-nbt properties can not modify nbt!");
    }
}
