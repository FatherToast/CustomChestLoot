package toast.ccl;

import java.io.File;
import java.io.FileWriter;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StringUtils;
import net.minecraft.util.WeightedRandomChestContent;

import com.google.gson.JsonObject;

public class CommandInfo extends CommandBase {
    // Returns true if the given command sender is allowed to use this command.
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return MinecraftServer.getServer().isSinglePlayer() || super.canCommandSenderUseCommand(sender);
    }

    // The command name.
    @Override
    public String getCommandName() {
        return "cclinfo";
    }

    // Returns the help string.
    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/cclinfo - writes the currently held item to a file in json format (ready to be added to a loot list).";
    }

    // Executes the command.
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityPlayerMP player = CommandBase.getCommandSenderAsPlayer(sender);

        ItemStack held = player.getHeldItem();
        if (held == null) {
            sender.addChatMessage(new ChatComponentText("[CCL Info] You must be holding the item you want info for!"));
        	return;
        }

        char[] fileNameArray = StringUtils.stripControlCodes(held.getDisplayName()).toCharArray();
        String fileName = "";
        for (char letter : fileNameArray) {
            fileName += Character.isLetterOrDigit(letter) ? Character.toString(letter) : "_";
        }

        try {
            FileHelper.INFO_DIRECTORY.mkdirs();
            File lootFile = new File(FileHelper.INFO_DIRECTORY, fileName + FileHelper.FILE_EXT);
            if (lootFile.exists()) {
                int attempt = 0;
                for (; attempt < 100; attempt++)
                    if (! (lootFile = new File(FileHelper.INFO_DIRECTORY, fileName + attempt + FileHelper.FILE_EXT)).exists()) {
                        break;
                    }
                if (attempt >= 100) {
                    sender.addChatMessage(new ChatComponentText("[CCL Info] Too many similarly named files exist! Try deleting some."));
                    _CustomChestLootMod.logWarning("Failed to generate loot info for \"" + StringUtils.stripControlCodes(held.getDisplayName()) + "\"!");
                    return;
                }
                fileName += attempt;
            }

			JsonObject lootInfo = CustomChestLoot.duplicateLootItem(new WeightedRandomChestContent(held, held.stackSize, held.stackSize, 1));

            lootFile.createNewFile();
            FileWriter out = new FileWriter(lootFile);
            out.write(FileHelper.getGsonFormatter().toJson(lootInfo).replace("\u00a7", "\\u00a7"));
            out.close();

            sender.addChatMessage(new ChatComponentText("[CCL Info] Generated loot info file \"" + fileName + "\" at:"));
            sender.addChatMessage(new ChatComponentText("    " + lootFile.getAbsolutePath()));
        }
        catch (ChestLootException ex) {
            throw ex;
        }
        catch (Exception ex) {
            sender.addChatMessage(new ChatComponentText("[CCL Info] An unknown error prevented the loot info file from being properly generated. See the console output for more information."));
            _CustomChestLootMod.logWarning("Failed to generate default properties file for \"" + StringUtils.stripControlCodes(held.getDisplayName()) + "\"!");
            ex.printStackTrace();
        }
    }
}
