package com.chunksending;

import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;

public interface IChunksendingPlayer
{
    boolean attachToPending(ChunkPos pos, IPacket<?> packet);
}
