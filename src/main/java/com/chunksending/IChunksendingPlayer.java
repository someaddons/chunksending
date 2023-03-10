package com.chunksending;

import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;

public interface IChunksendingPlayer
{
    boolean attachToPending(ChunkPos pos, Packet<?> packet);
}
