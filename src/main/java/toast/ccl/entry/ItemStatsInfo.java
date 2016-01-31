package toast.ccl.entry;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ItemStatsInfo
{
	// The running list of item stacks generated.
	public final ArrayList<ItemStack> itemList;
	// The inventory being generated into.
	public final IInventory inventory;

    // The block info this is a part of.
    public final Object parent;
    // The item currently being initialized.
    public final ItemStack theItem;
    // The world that the item is dropping into.
    public final World theWorld;
    // The random number generator.
    public final Random random;

    // The tile entity the item is spawning in (if applicable).
    public final TileEntity tileEntity;
    // The entity the item is spawning in (if applicable).
    public final Entity entity;
    // Position the item is spawning at (if applicable).
    public final int x, y, z;

    public ItemStatsInfo(ItemStack item, ArrayList<ItemStack> items, Random random, IInventory inventory, Object blockInfo) {
    	this.itemList = items;
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
}