package com.github.lunatrius.schematica.world.chunk;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class GenerateSchematicContainer extends SchematicContainer {

    private String fileName;
    private Vector3i startPos;

    public GenerateSchematicContainer(ISchematic schematic, ICommandSender player, World world, Vector3i startPos, String fileName) {
        super(schematic, player, world, startPos);

        this.fileName = fileName;
        this.startPos = startPos;
    }

    @Override
    public void first() {
        tellSender(Names.Command.Generate.Message.STARTED, getChunkCount(), fileName);
    }

    @Override
    protected void processSingleChunk(int chunkX, int chunkZ) {
        Reference.logger.debug(String.format("Loading chunk at [%d,%d] from %s", chunkX, chunkZ, this.fileName));
        Schematica.proxy.generateSchematicChunk(getSchematic(), getWorld(), chunkX, chunkZ, startPos);
    }

    @Override
    public void complete() {
        for (NBTTagCompound entity : getSchematic().getEntityData()) {
            entity.removeTag("UUIDMost");
            entity.removeTag("PersistentIDMSB");
            Entity newEntity = EntityList.createEntityFromNBT(entity, getWorld());
            newEntity.posX += startPos.x;
            newEntity.posY += startPos.y;
            newEntity.posZ += startPos.z;
            newEntity.setPosition(newEntity.posX, newEntity.posY, newEntity.posZ);

            getWorld().spawnEntityInWorld(newEntity);
        }
        final String message = Names.Command.Generate.Message.SUCCESSFUL;
        tellSender(message, fileName);
    }
}
