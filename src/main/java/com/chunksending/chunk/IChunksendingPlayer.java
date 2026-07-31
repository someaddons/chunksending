package com.chunksending.chunk;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public interface IChunksendingPlayer
{
    void attachToPending(final ChunkMap chunkMap, final ServerPlayer player, final ChunkPos pos);
    boolean isSending(final ChunkPos pos);
    int chunksQueued();
}
