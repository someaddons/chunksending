package com.chunksending.chunk;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;

public interface IChunkPacketCache
{
    ClientboundLevelChunkWithLightPacket getCachedPacket();

    void setCachedPacket(ClientboundLevelChunkWithLightPacket packet);

    void clearCachedPacket();
}
