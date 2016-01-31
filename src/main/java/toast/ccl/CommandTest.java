package toast.ccl;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

public class CommandTest extends CommandBase {
    // Returns true if the given command sender is allowed to use this command.
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return MinecraftServer.getServer().isSinglePlayer() || super.canCommandSenderUseCommand(sender);
    }

    // The command name.
    @Override
    public String getCommandName() {
        return "ccltest";
    }

    // Returns the help string.
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ccltest <loot list> - reloads loot and generates a chest at your feet using the specified loot list.";
    }

    // Executes the command.
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
    	String lootList;
    	if (args.length > 0) {
    		lootList = args[0];
    	}
    	else {
    		lootList = "dungeonChest";
    	}

        sender.addChatMessage(new ChatComponentText("Reloading custom chest loot!"));

        _CustomChestLootMod.log("Reloading custom chest loot...");
        _CustomChestLootMod.log("Loaded " + FileHelper.load() + " loot lists!");

        sender.addChatMessage(new ChatComponentText("Generating chest with loot list \"" + lootList + "\"."));

        ChunkCoordinates coords = sender.getPlayerCoordinates();
        sender.getEntityWorld().setBlock(coords.posX, coords.posY, coords.posZ, Blocks.chest, 0, 2);
        TileEntityChest tileEntity = (TileEntityChest) sender.getEntityWorld().getTileEntity(coords.posX, coords.posY, coords.posZ);
        if (tileEntity != null) {
            WeightedRandomChestContent.generateChestContents(sender.getEntityWorld().rand, ChestGenHooks.getItems(lootList, sender.getEntityWorld().rand), tileEntity, ChestGenHooks.getCount(lootList, sender.getEntityWorld().rand));
            _CustomChestLootMod.log("Generated \"" + lootList + "\" chest at (" + coords.posX + "," + coords.posY + "," + coords.posZ + ")");
        }
        else {
            sender.addChatMessage(new ChatComponentText("Error generating chest, failed to fetch tile entity!"));
        }
    }
}
