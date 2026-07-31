package com.chunksending.event;

import com.chunksending.ChunkSending;
import com.chunksending.chunk.IChunksendingPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandler
{
    public static int maxChunksPerPlayer = 15;

    @SubscribeEvent()
    public static void onServerTick(TickEvent.ServerTickEvent tickEvent)
    {
        if (tickEvent.phase != TickEvent.Phase.START)
        {
            return;
        }

        int pendingPlayers = 0;
        int pendingChunks = 0;

        for (final ServerPlayer player : tickEvent.getServer().getPlayerList().getPlayers())
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
