package com.chunksending.event;

import com.chunksending.chunk.IChunkPacketCache;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.config.CommonConfiguration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class EventHandler
{
    private static final long                         packetCacheLifetime = TimeUnit.MINUTES.toNanos(5);
    private static final Map<IChunkPacketCache, Long> packetCachesToClear = new WeakHashMap<>();

    public static int maxChunksPerPlayer = 15;

    @SubscribeEvent()
    public static void onServerTick(TickEvent.ServerTickEvent tickEvent)
    {
        if (tickEvent.phase != TickEvent.Phase.START)
        {
            return;
        }

        clearExpiredPacketCaches();

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
                    pendingChunks += Math.min(CommonConfiguration.config.getCommonConfig().maxChunksPerTick, chunksQueued);
                }
            }
        }

        if (pendingPlayers == 0 || pendingChunks <= CommonConfiguration.config.getCommonConfig().maxChunksPerTickAll)
        {
            maxChunksPerPlayer = CommonConfiguration.config.getCommonConfig().maxChunksPerTick;
        }
        else
        {
            maxChunksPerPlayer = Math.max(1, CommonConfiguration.config.getCommonConfig().maxChunksPerTickAll / pendingPlayers);
        }
    }

    public static void addToClear(final IChunkPacketCache packetCache)
    {
        packetCachesToClear.put(packetCache, System.nanoTime() + packetCacheLifetime);
    }

    private static void clearExpiredPacketCaches()
    {
        if (packetCachesToClear.isEmpty())
        {
            return;
        }

        final long now = System.nanoTime();
        final Iterator<Map.Entry<IChunkPacketCache, Long>> iterator = packetCachesToClear.entrySet().iterator();

        while (iterator.hasNext())
        {
            final Map.Entry<IChunkPacketCache, Long> entry = iterator.next();
            final IChunkPacketCache packetCache = entry.getKey();
            if (packetCache == null || now - entry.getValue() >= 0)
            {
                if (packetCache != null)
                {
                    packetCache.clearCachedPacket();
                }
                iterator.remove();
            }
        }
    }
}
