package com.chunksending;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public interface IBatchedUpdateSender
{
    boolean attachToPending(ChunkPos pos, Packet<?> packet, final Player player);

    void reset();

    void tick(final ServerPlayer player);
}
