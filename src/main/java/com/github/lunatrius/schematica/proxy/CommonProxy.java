package com.github.lunatrius.schematica.proxy;

import com.github.lunatrius.core.util.vector.Vector3i;
import com.github.lunatrius.core.version.VersionChecker;
import com.github.lunatrius.schematica.api.ISchematic;
import com.github.lunatrius.schematica.command.CommandSchematicaGenerate;
import com.github.lunatrius.schematica.command.CommandSchematicaList;
import com.github.lunatrius.schematica.command.CommandSchematicaRemove;
import com.github.lunatrius.schematica.command.CommandSchematicaSave;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.handler.DownloadHandler;
import com.github.lunatrius.schematica.handler.QueueTickHandler;
import com.github.lunatrius.schematica.nbt.NBTHelper;
import com.github.lunatrius.schematica.nbt.TileEntityException;
import com.github.lunatrius.schematica.network.PacketHandler;
import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.schematica.world.SchematicWorld;
import com.github.lunatrius.schematica.world.chunk.FileSaveSchematicContainer;
import com.github.lunatrius.schematica.world.chunk.GenerateSchematicContainer;
import com.github.lunatrius.schematica.world.chunk.SchematicContainer;
import com.github.lunatrius.schematica.world.schematic.SchematicFormat;
import com.github.lunatrius.schematica.world.schematic.SchematicUtil;
import com.github.lunatrius.schematica.world.storage.Schematic;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.block.Block;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.io.File;

public abstract class CommonProxy {
    public boolean isSaveEnabled = true;
    public boolean isLoadEnabled = true;

    public void preInit(FMLPreInitializationEvent event) {
        Reference.logger = event.getModLog();
        ConfigurationHandler.init(event.getSuggestedConfigurationFile());

        VersionChecker.registerMod(event.getModMetadata(), Reference.FORGE);
    }

    public void init(FMLInitializationEvent event) {
        PacketHandler.init();

        FMLCommonHandler.instance().bus().register(QueueTickHandler.INSTANCE);
        FMLCommonHandler.instance().bus().register(DownloadHandler.INSTANCE);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandSchematicaSave());
        event.registerServerCommand(new CommandSchematicaList());
        event.registerServerCommand(new CommandSchematicaRemove());
        event.registerServerCommand(new CommandSchematicaGenerate());
    }

    public void createFolders() {
        if (!ConfigurationHandler.schematicDirectory.exists()) {
            if (!ConfigurationHandler.schematicDirectory.mkdirs()) {
                Reference.logger.info("Could not create schematic directory [%s]!", ConfigurationHandler.schematicDirectory.getAbsolutePath());
            }
        }
    }

    public abstract File getDataDirectory();

    public void resetSettings() {
        this.isSaveEnabled = true;
        this.isLoadEnabled = true;
    }

    public void copyChunkToSchematic(final ISchematic schematic, final World world, final int chunkX, final int chunkZ, final int minX, final int maxX, final int minY, final int maxY, final int minZ, final int maxZ) {
        final int localMinX = minX < (chunkX << 4) ? 0 : (minX & 15);
        final int localMaxX = maxX > ((chunkX << 4) + 15) ? 15 : (maxX & 15);
        final int localMinZ = minZ < (chunkZ << 4) ? 0 : (minZ & 15);
        final int localMaxZ = maxZ > ((chunkZ << 4) + 15) ? 15 : (maxZ & 15);

        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

        for (int i = 0; i < chunk.entityLists.length; i++) {
            for (Object entityObj : chunk.entityLists[i]) {
                Entity entity = (Entity)entityObj;
                entity.posX -= minX;
                entity.posY -= minY;
                entity.posZ -= minZ;

                schematic.addEntity(entity);

                entity.posX += minX;
                entity.posY += minY;
                entity.posZ += minZ;
            }
        }

        for (int chunkLocalX = localMinX; chunkLocalX <= localMaxX; chunkLocalX++) {
            for (int chunkLocalZ = localMinZ; chunkLocalZ <= localMaxZ; chunkLocalZ++) {
                for (int y = minY; y <= maxY; y++) {
                    final int x = chunkLocalX | (chunkX << 4);
                    final int z = chunkLocalZ | (chunkZ << 4);

                    final int localX = x - minX;
                    final int localY = y - minY;
                    final int localZ = z - minZ;

                    try {
                        final Block block = world.getBlock(x, y, z);
                        final int metadata = world.getBlockMetadata(x, y, z);
                        final boolean success = schematic.setBlock(localX, localY, localZ, block, metadata);

                        if (success && block.hasTileEntity(metadata)) {
                            final TileEntity tileEntity = world.getTileEntity(x, y, z);
                            if (tileEntity != null) {
                                try {
                                    final TileEntity reloadedTileEntity = NBTHelper.reloadTileEntity(tileEntity, minX, minY, minZ);
                                    schematic.setTileEntity(localX, localY, localZ, reloadedTileEntity);
                                } catch (TileEntityException tee) {
                                    Reference.logger.error(String.format("Error while trying to save tile entity '%s'!", tileEntity), tee);
                                    schematic.setBlock(localX, localY, localZ, Blocks.bedrock);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Reference.logger.error("Something went wrong!", e);
                    }
                }
            }
        }
    }

    public boolean generateSchematic(ICommandSender sender, File directory, String filename, World world, Vector3i from) {
        ISchematic schematic = SchematicFormat.readFromFile(directory, filename);
        if (schematic == null) {
            return false;
        }

        final SchematicContainer container = new GenerateSchematicContainer(schematic, sender, world, from, filename);
        QueueTickHandler.INSTANCE.queueSchematic(container);
        return true;
    }

    public void generateSchematicChunk(ISchematic schematic, World world, int chunkX, int chunkZ, Vector3i from) {
        int maxX = from.x + schematic.getWidth()-1;
        int maxZ = from.z + schematic.getLength()-1;
        int maxY = from.y + schematic.getHeight()-1;
        final int localMinX = from.x < (chunkX << 4) ? 0 : (from.x & 15);
        final int localMaxX = maxX > ((chunkX << 4) + 15) ? 15 : (maxX & 15);
        final int localMinZ = from.z < (chunkZ << 4) ? 0 : (from.z & 15);
        final int localMaxZ = maxZ > ((chunkZ << 4) + 15) ? 15 : (maxZ & 15);

        for (int x = localMinX; x <= localMaxX; x++) {
            for (int z = localMinZ; z <= localMaxZ; z++) {
                for (int y = from.y; y <= maxY; y++) {
                    int worldX = x | (chunkX << 4);
                    int worldZ = z | (chunkZ << 4);
                    Block block = schematic.getBlock(worldX - from.x, y - from.y, worldZ - from.z);
                    int meta = schematic.getBlockMetadata(worldX - from.x, y - from.y, worldZ - from.z);
                    world.setBlock(worldX, y, worldZ, block, meta, 3);

                    TileEntity te = schematic.getTileEntity(worldX - from.x, y - from.y, worldZ - from.z);

                    if (te != null) {
                        try {
                            te = NBTHelper.reloadTileEntity(te, -from.x, -from.y, -from.z);
                            world.setTileEntity(worldX, y, worldZ, te);
                        } catch (TileEntityException ex) {
                            Reference.logger.error(String.format("Error while trying to generate tile entity '%s'!", te), ex);
                            world.setBlock(worldX, y, worldZ, Blocks.bedrock);
                        }
                    }
                }
            }
        }
    }

    public boolean saveSchematic(EntityPlayer player, File directory, String filename, World world, Vector3i from, Vector3i to) {
        try {
            String iconName = "";

            try {
                String[] parts = filename.split(";");
                if (parts.length == 2) {
                    iconName = parts[0];
                    filename = parts[1];
                }
            } catch (Exception e) {
                Reference.logger.error("Failed to parse icon data!", e);
            }

            final int minX = Math.min(from.x, to.x);
            final int maxX = Math.max(from.x, to.x);
            final int minY = Math.min(from.y, to.y);
            final int maxY = Math.max(from.y, to.y);
            final int minZ = Math.min(from.z, to.z);
            final int maxZ = Math.max(from.z, to.z);

            final short width = (short) (Math.abs(maxX - minX) + 1);
            final short height = (short) (Math.abs(maxY - minY) + 1);
            final short length = (short) (Math.abs(maxZ - minZ) + 1);

            final ISchematic schematic = new Schematic(SchematicUtil.getIconFromName(iconName), width, height, length);
            final SchematicContainer container = new FileSaveSchematicContainer(schematic, player, world, new File(directory, filename), new Vector3i(minX, minY, minZ));
            QueueTickHandler.INSTANCE.queueSchematic(container);

            return true;
        } catch (Exception e) {
            Reference.logger.error("Failed to save schematic!", e);
        }
        return false;
    }

    public abstract boolean loadSchematic(EntityPlayer player, File directory, String filename);

    public abstract void setActiveSchematic(SchematicWorld world);

    public abstract void setActiveSchematic(SchematicWorld world, EntityPlayer player);

    public abstract SchematicWorld getActiveSchematic();

    public abstract SchematicWorld getActiveSchematic(EntityPlayer player);

    public abstract boolean isPlayerQuotaExceeded(EntityPlayer player);

    public abstract File getPlayerSchematicDirectory(EntityPlayer player, boolean privateDirectory);
}
