package com.chunksending.chunk;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

/**
 * Holds data for the method call
 */
public record Data(
    ChunkMap chunkMap,
    ServerPlayer player,
    ChunkPos chunkPos)
{
    /**
     * @param chunkMap
     * @param player
     * @param chunkPos
     */
    public Data
    {
    }

    @Override
    public String toString()
    {
        return "Data[" +
            "chunkMap=" + chunkMap + ", " +
            "player=" + player + ", " +
            "chunkPos=" + chunkPos + ']';
    }
}
