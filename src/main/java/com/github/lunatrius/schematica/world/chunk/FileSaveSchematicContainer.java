package com.github.lunatrius.schematica.world.chunk;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;

import java.io.File;

public class FileSaveSchematicContainer extends SchematicContainer {

    private final File file;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    public FileSaveSchematicContainer(ISchematic schematic, ICommandSender player, World world, File file, Vector3i startPos) {
        super(schematic, player, world, startPos);
        this.file = file;

        this.minX = startPos.x;
        this.minY = startPos.y;
        this.minZ = startPos.z;
        this.maxX = startPos.x + schematic.getWidth();
        this.maxY = startPos.y + schematic.getHeight();
        this.maxZ = startPos.z + schematic.getLength();
    }

    @Override
    public void first() {
        tellSender(Names.Command.Save.Message.SAVE_STARTED, getChunkCount(), file.getName());
    }

    @Override
    protected void processSingleChunk(int chunkX, int chunkZ) {
        Reference.logger.debug(String.format("Copying chunk at [%d,%d] into %s", chunkX, chunkZ, this.file.getName()));
        Schematica.proxy.copyChunkToSchematic(getSchematic(), getWorld(), chunkX, chunkZ, this.minX, this.maxX, this.minY, this.maxY, this.minZ, this.maxZ);
    }

    @Override
    public void complete() {
        final boolean success = SchematicFormat.writeToFile(file, getSchematic());
        final String message = success ? Names.Command.Save.Message.SAVE_SUCCESSFUL : Names.Command.Save.Message.SAVE_FAILED;
        tellSender(message, file.getName());
    }
}
