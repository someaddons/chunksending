package com.chunksending.event;

import com.chunksending.ChunkSending;
import com.chunksending.chunk.IChunksendingPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class EventHandler
{
    public static int maxChunksPerPlayer = 15;

    public static void onServerTick(MinecraftServer minecraftServer)
    {
        int pendingPlayers = 0;
        int pendingChunks = 0;

        for (final ServerPlayer player : minecraftServer.getPlayerList().getPlayers())
        {
            if (player instanceof IChunksendingPlayer chunksendingPlayer)
            {
                final int chunksQueued = chunksendingPlayer.chunksQueued();
                if (chunksQueued > 0)
                {
                    pendingPlayers++;
                    pendingChunks += Math.min(ChunkSending.config.getCommonConfig().maxChunksPerTick, chunksQueued);
                }
            }
        }

        if (pendingPlayers == 0 || pendingChunks <= ChunkSending.config.getCommonConfig().maxChunksPerTickAll)
        {
            maxChunksPerPlayer = ChunkSending.config.getCommonConfig().maxChunksPerTick;
        }
        else
        {
            maxChunksPerPlayer = Math.max(1, ChunkSending.config.getCommonConfig().maxChunksPerTickAll / pendingPlayers);
        }
    }
}
