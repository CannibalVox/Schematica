package com.github.lunatrius.schematica.command;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;

import java.io.File;

public class CommandSchematicaGenerate extends CommandSchematicaBase {
    @Override
    public String getCommandName() {
        return Names.Command.Generate.NAME;
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_) {
        return Names.Command.Generate.Message.USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length < 4) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        Vector3i from = new Vector3i();
        String filename;
        String name;

        try {
            name = arguments[0];
            from.set(Integer.parseInt(arguments[1]), Integer.parseInt(arguments[2]), Integer.parseInt(arguments[3]));

            filename = String.format("%s.schematic", name);
        } catch (NumberFormatException exception) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        Reference.logger.info(String.format("Generating schematic from %s to %s", filename, from));
        final File schematicDirectory = ConfigurationHandler.schematicDirectory;

        if (!schematicDirectory.exists()) {
            schematicDirectory.mkdirs();
        }

        try {
            if (Schematica.proxy.generateSchematic(sender, schematicDirectory, filename, sender.getEntityWorld(), from))
                sender.addChatMessage(new ChatComponentTranslation(Names.Command.Generate.Message.SUCCESSFUL, name));
            else
                sender.addChatMessage(new ChatComponentTranslation(Names.Command.Generate.Message.FAILED, name));
        } catch (Exception e) {
            throw new CommandException(Names.Command.Generate.Message.FAILED, name);
        }
    }
}
