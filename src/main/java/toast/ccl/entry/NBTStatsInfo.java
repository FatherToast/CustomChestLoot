package toast.ccl.entry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class NBTStatsInfo
{
	// The inventory being generated into.
	public final IInventory inventory;

    // The block info this is a part of.
    public final Object parent;
    // The item currently being initialized.
    public final ItemStack theItem;
    // The world that the item is dropping into.
    public final World theWorld;
    // The world's random number generator.
    public final Random random;

    // The tile entity the item is spawning in (if applicable).
    public final TileEntity tileEntity;
    // The entity the item is spawning in (if applicable).
    public final Entity entity;
    // Position the item is spawning at (if applicable).
    public final int x, y, z;

    // List containing all tags that will be added to the mob/item.
    private final ArrayList<NBTWrapper> tags = new ArrayList<NBTWrapper>();

    public NBTStatsInfo(ItemStack item, Random random, IInventory inventory, Object blockInfo) {
    	this.inventory = inventory;

        this.parent = blockInfo;
        this.theItem = item;
        this.random = random;

        // Try to get helpful info for conditionals
        if (inventory instanceof TileEntity) {
        	this.tileEntity = (TileEntity) inventory;
        	this.entity = null;

        	this.theWorld = this.tileEntity.getWorldObj();
        	this.x = this.tileEntity.xCoord;
        	this.y = this.tileEntity.yCoord;
        	this.z = this.tileEntity.zCoord;
        }
        else if (inventory instanceof Entity) {
        	this.tileEntity = null;
        	this.entity = (Entity) inventory;

        	this.theWorld = this.entity.worldObj;
        	this.x = (int) Math.floor(this.entity.posX);
        	this.y = (int) Math.floor(this.entity.posY);
        	this.z = (int) Math.floor(this.entity.posZ);
        }
        else {
        	this.tileEntity = null;
        	this.entity = null;

        	this.theWorld = null;
        	this.x = 0;
        	this.y = -1;
        	this.z = 0;
        }
    }

    // Adds a tag to this info.
    public void addTag(String name, NBTBase tag) {
        this.tags.add(new NBTWrapper(name, tag));
    }

    // Writes all tags to the given tag compound and returns that compound.
    public NBTTagCompound writeTo(NBTTagCompound compound) {
        for (NBTWrapper wrapper : this.tags) {
            if (wrapper.getTag() == null) {
            	compound.removeTag(wrapper.getName());
            }
            else if (wrapper.getTag().getClass() == NBTTagCompound.class) {
                this.writeCompound(compound, wrapper);
            }
            else {
                compound.setTag(wrapper.getName(), wrapper.getTag());
            }
        }
        return compound;
    }
    public NBTTagList writeTo(NBTTagList list) {
        for (NBTWrapper wrapper : this.tags) {
        	if (wrapper.getTag() == null) {
        		list.removeTag(list.tagCount() - 1);
        	}
        	else {
				list.appendTag(wrapper.getTag());
			}
        }
        return list;
    }

    // Called recursively to copy all the NBT tags from a wrapped compound.
    private void writeCompound(NBTTagCompound compound, NBTWrapper wrapper) {
        NBTTagCompound copyTo = compound.getCompoundTag(wrapper.getName());
        if (!compound.hasKey(wrapper.getName())) {
            compound.setTag(wrapper.getName(), copyTo);
        }

        NBTTagCompound copyFrom = (NBTTagCompound) wrapper.getTag();
        for (String name : (Collection<String>) copyFrom.func_150296_c()) {
            NBTBase tag = copyFrom.getTag(name);
            if (tag.getClass() == NBTTagCompound.class) {
                this.writeCompound(copyTo, new NBTWrapper(name, tag));
            }
            else {
                copyTo.setTag(name, tag.copy());
            }
        }
    }

    /** Wrapper class to store an NBT tag with its name. */
    private static class NBTWrapper {
        private final String name;
        private final NBTBase tag;

        public NBTWrapper(String name, NBTBase tag) {
            this.name = name;
            this.tag = tag;
        }

        /** @return the name tag for the wrapped NBTBase. Empty string only if in a list. */
        public String getName() {
            return this.name;
        }

        /** @return the wrapped NBTBase instance. */
        public NBTBase getTag() {
            return this.tag;
        }
    }
}
