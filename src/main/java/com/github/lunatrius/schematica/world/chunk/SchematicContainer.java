package com.github.lunatrius.schematica.world.chunk;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.schematica.Schematica;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.reference.Names;
import com.github.lunatrius.schematica.reference.Reference;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import java.io.File;

public abstract class SchematicContainer {
    private final ISchematic schematic;
    private final ICommandSender player;
    private final World world;

    private final int minChunkX;
    private final int maxChunkX;
    private final int minChunkZ;
    private final int maxChunkZ;

    private int curChunkX;
    private int curChunkZ;

    private final int chunkCount;

    protected int getChunkCount() {
        return chunkCount;
    }

    protected ISchematic getSchematic() { return schematic; }
    protected World getWorld() { return world; }

    public SchematicContainer(ISchematic schematic, ICommandSender player, World world, Vector3i startPos) {
        this.schematic = schematic;
        this.player = player;
        this.world = world;

        int minX = startPos.x;
        int minZ = startPos.z;
        int maxX = minX + schematic.getWidth();
        int maxZ = minZ + schematic.getLength();

        this.minChunkX = minX >> 4;
        this.maxChunkX = maxX >> 4;
        this.minChunkZ = minZ >> 4;
        this.maxChunkZ = maxZ >> 4;

        this.curChunkX = this.minChunkX;
        this.curChunkZ = this.minChunkZ;

        this.chunkCount = (this.maxChunkX - this.minChunkX + 1) * (this.maxChunkZ - this.minChunkZ + 1);
    }

    public void next() {
        if (!hasNext()) {
            return;
        }

        processSingleChunk(curChunkX, curChunkZ);

        this.curChunkX++;
        if (this.curChunkX > this.maxChunkX) {
            this.curChunkX = this.minChunkX;
            this.curChunkZ++;
        }
    }

    public abstract void first();
    protected abstract void processSingleChunk(int chunkX, int chunkZ);
    public abstract void complete();

    public boolean isFirst() {
        return this.curChunkX == this.minChunkX && this.curChunkZ == this.minChunkZ;
    }

    public boolean hasNext() {
        return this.curChunkX <= this.maxChunkX && this.curChunkZ <= this.maxChunkZ;
    }

    protected void tellSender(String text, Object... params) {
        final ChatComponentTranslation chatComponent = new ChatComponentTranslation(text, params);
        player.addChatMessage(chatComponent);
    }
}
