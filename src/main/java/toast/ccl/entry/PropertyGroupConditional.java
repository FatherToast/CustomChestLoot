package toast.ccl.entry;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import toast.ccl.FileHelper;
import toast.ccl.IPropertyReader;
import toast.ccl._CustomChestLootMod;

import com.google.gson.JsonObject;

public class PropertyGroupConditional extends PropertyGroup {
    // The method to call for difficulty checks, if found.
    private static Method worldDifficultyMethod;

    static {
        try {
            PropertyGroupConditional.worldDifficultyMethod = Class.forName("toast.apocalypse.WorldDifficultyManager").getMethod("getWorldDifficulty");
            _CustomChestLootMod.log("Successfully hooked into Apocalypse's world difficulty!");
        }
        catch (Exception ex) {
            // Do nothing
        }
    }

    // Returns true if the category can be executed.
    public static boolean isCategoryActive(boolean invert, String category, Object info) {
        if (info instanceof ItemStatsInfo) {
        	ItemStatsInfo itemStats = (ItemStatsInfo) info;
            return PropertyGroupConditional.isCategoryActive(invert, category,
            		itemStats.theItem, itemStats.inventory, itemStats.theWorld, itemStats.tileEntity, itemStats.entity,
            		itemStats.x, itemStats.y, itemStats.z);
        }
        if (info instanceof NBTStatsInfo) {
        	NBTStatsInfo nbtStats = (NBTStatsInfo) info;
            return PropertyGroupConditional.isCategoryActive(invert, category,
            		nbtStats.theItem, nbtStats.inventory, nbtStats.theWorld, nbtStats.tileEntity, nbtStats.entity,
            		nbtStats.x, nbtStats.y, nbtStats.z);
        }
        return false;
    }
    public static boolean isCategoryActive(boolean invert, String category, ItemStack item, IInventory inventory, World world, TileEntity tileEntity, Entity entity, int x, int y, int z) {
    	if (category.startsWith("is_tile_entity_")) {
            try {
            	NBTTagCompound tag = new NBTTagCompound();
    			tileEntity.writeToNBT(tag);
            	return tag.getString("id").equals(category.substring(15)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
    	if (category.startsWith("check_nbt_")) {
            try {
                if (tileEntity == null)
                    return invert;
                String[] path = category.substring(10).split("/");
                String[] data = PropertyGroupConditional.getOperatorData(path[path.length - 1]);
                path[path.length - 1] = data[0];

                return PropertyGroupConditional.compareNBT(tileEntity, path, data) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }

        if (category.equals("raining")) {
            try {
            	return world.isRaining() != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.equals("thundering")) {
            try {
            	return world.isThundering() != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.equals("can_see_sky")) {
            try {
            	return world.canBlockSeeTheSky(x, y, z) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("moon_phase_")) {
            try {
            	return world.provider.getMoonPhase(world.getWorldTime()) == PropertyGroupConditional.getMoonPhaseId(category.substring(11)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("beyond_")) {
            try {
                double distance = Double.parseDouble(category.substring(7));
                ChunkCoordinates spawnPoint = world.getSpawnPoint();
                return (y >= 0 && spawnPoint.getDistanceSquared(x, y, z) > distance * distance) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("difficulty_")) {
            try {
            	return world.difficultySetting == PropertyGroupConditional.getDifficulty(category.substring(11)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("past_world_difficulty_")) {
            try {
                long difficulty = (long) (Double.parseDouble(category.substring(22)) * 24000L);
                if (PropertyGroupConditional.worldDifficultyMethod == null) {
                    category = "past_world_time_" + difficulty;
                }
                else
                    return ((Long) PropertyGroupConditional.worldDifficultyMethod.invoke(null)).longValue() > difficulty != invert;
            }
            catch (Exception ex) {
                return invert;
            }
        }
        if (category.startsWith("past_day_time_")) {
            try {
                return (int) (world.getWorldInfo().getWorldTime() % 24000L) > Integer.parseInt(category.substring(14)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("past_world_time_")) {
            try {
                return world.getWorldInfo().getWorldTime() > Long.parseLong(category.substring(16)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("in_dimension_")) {
            try {
                return world.provider.dimensionId == Integer.parseInt(category.substring(13)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("in_biome_")) {
            try {
                return (y >= 0 && world.getBiomeGenForCoords(x, z).biomeID == Integer.parseInt(category.substring(9))) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("touching_block_")) {
            try {
                Block adjBlock = FileHelper.readBlock(category.substring(15), tileEntity == null ? "(null)" : tileEntity.getClass().getName(), true);
                return (world.getBlock(x, y - 1, z) == adjBlock || world.getBlock(x, y + 1, z) == adjBlock ||
                        world.getBlock(x - 1, y, z) == adjBlock || world.getBlock(x + 1, y, z) == adjBlock ||
                        world.getBlock(x, y, z - 1) == adjBlock || world.getBlock(x, y, z + 1) == adjBlock) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        if (category.startsWith("below_")) {
            try {
                return y < Integer.parseInt(category.substring(6)) != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }

        if (category.startsWith("player_online_")) {
            try {
                return world.getPlayerEntityByName(category.substring(14)) != null != invert;
            }
            catch (Exception ex) {
                // Do nothing
            }
            return invert;
        }
        throw new RuntimeException("[ERROR] Conditional property has invalid condition! for " + tileEntity.getClass().getName());
    }

    // Returns a string array with the path end for the left-hand operand, the operator string, and the right-hand operand.
    private static String[] getOperatorData(String last) {
        String[] operators = { "==", ">", "<", ">=", "<=" };
        String[] split;
        for (String operator : operators) {
            split = last.split(operator, 2);
            if (split.length == 2)
                return new String[] { split[0], operator, split[1] };
        }
        return new String[] { last, null, null };
    }

    // Compares the actual value given by the NBT path to a value, based on the operator string in the data array.
    private static boolean compareNBT(TileEntity tileEntity, String[] path, String[] data) {
        NBTTagCompound tag = new NBTTagCompound();
        tileEntity.writeToNBT(tag);
        return PropertyGroupConditional.compareNBT(tag, path, data);
    }
    @SuppressWarnings("unused")
	private static boolean compareNBT(Entity entity, String[] path, String[] data) {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBT(tag);
        return PropertyGroupConditional.compareNBT(tag, path, data);
    }
    private static boolean compareNBT(NBTTagCompound baseTag, String[] path, String[] data) {
        // Step to tag at the end, tag becomes null if the target does not exist
        NBTBase tag = baseTag;
        for (String pathStep : path) {
            if (tag instanceof NBTTagCompound) {
                if (!((NBTTagCompound) tag).hasKey(pathStep)) {
                    tag = null;
                    break;
                }
                tag = ((NBTTagCompound) tag).getTag(pathStep);
            }
            else if (tag instanceof NBTTagList) {
                int index = Integer.parseInt(pathStep);
                if (((NBTTagList) tag).tagCount() <= index) {
                    tag = null;
                    break;
                }
                // Only way to directly get an element from the list
                // The entity is not read from the tag again, anyway
                tag = ((NBTTagList) tag).removeTag(index);
            }
            else
                return false;
        }

        // Compare the actual to the value
        if (data[1] == null) // boolean check
            return tag != null && ((NBTBase.NBTPrimitive) tag).func_150290_f() == 1;
        double value;
        try {
        	value = Double.parseDouble(data[2]);
        }
        catch (NumberFormatException ex) { // String check
        	return data[1].equals("==") && data[2].equals(((NBTTagString) tag).func_150285_a_());
        }
        double actual;
        if (tag == null) {
            actual = 0.0;
        }
        else {
            actual = ((NBTBase.NBTPrimitive) tag).func_150286_g();
        }

        if (data[1].equals("=="))
            return actual == value;
        if (data[1].equals(">"))
            return actual > value;
        if (data[1].equals("<"))
            return actual < value;
        if (data[1].equals(">="))
            return actual >= value;
        if (data[1].equals("<="))
            return actual <= value;
        return false;
    }

    // Returns the moon phase id from the given string.
    private static int getMoonPhaseId(String phase) {
        if (phase.equalsIgnoreCase("FULL"))
            return 0;
        if (phase.equalsIgnoreCase("WANING_GIBBOUS"))
            return 1;
        if (phase.equalsIgnoreCase("THIRD_QUARTER") || phase.equalsIgnoreCase("WANING_HALF"))
            return 2;
        if (phase.equalsIgnoreCase("WANING_CRESCENT"))
            return 3;
        if (phase.equalsIgnoreCase("NEW"))
            return 4;
        if (phase.equalsIgnoreCase("WAXING_CRESCENT"))
            return 5;
        if (phase.equalsIgnoreCase("FIRST_QUARTER") || phase.equalsIgnoreCase("WAXING_HALF"))
            return 6;
        if (phase.equalsIgnoreCase("WAXING_GIBBOUS"))
            return 7;
        try {
            return Integer.parseInt(phase) % 8;
        }
        catch (Exception ex) {
            return -1;
        }
    }

    // Parses the world difficulty from a string.
    private static EnumDifficulty getDifficulty(String id) {
        if (id.equalsIgnoreCase("PEACEFUL"))
            return EnumDifficulty.PEACEFUL;
        else if (id.equalsIgnoreCase("EASY"))
            return EnumDifficulty.EASY;
        else if (id.equalsIgnoreCase("NORMAL"))
            return EnumDifficulty.NORMAL;
        else if (id.equalsIgnoreCase("HARD"))
            return EnumDifficulty.HARD;
        else {
            try {
                return EnumDifficulty.getDifficultyEnum(Integer.parseInt(id));
            }
            catch (Exception ex) {
                return null;
            }
        }
    }

    // True if this should only execute when its category would not.
    private final boolean inverted;
    // The category name. Used to check if this property should execute.
    private final String category;

    public PropertyGroupConditional(String path, JsonObject root, int index, JsonObject node, IPropertyReader loader, String function, boolean invert) {
        super(path, root, index, node, loader);
        this.inverted = invert;
        this.category = function;
    }

    // Modifies the item.
    @Override
    public void modifyItem(ItemStatsInfo itemStats) {
        if (PropertyGroupConditional.isCategoryActive(this.inverted, this.category, itemStats.parent)) {
            super.modifyItem(itemStats);
        }
    }

    // Adds any NBT tags to the list.
    @Override
    public void addTags(NBTStatsInfo nbtStats) {
        if (PropertyGroupConditional.isCategoryActive(this.inverted, this.category, nbtStats.parent)) {
            super.addTags(nbtStats);
        }
    }
}
