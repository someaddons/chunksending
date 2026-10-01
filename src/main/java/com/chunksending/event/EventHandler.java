package com.chunksending.event;

import com.chunksending.chunk.IChunkPacketCache;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.config.CommonConfiguration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

public class EventHandler
{
    private static final long                                      packetCacheLifetime = TimeUnit.MINUTES.toNanos(5);
    private static final Map<IChunkPacketCache, PacketCacheExpiry> packetCachesToClear = new WeakHashMap<>();
    private static final Queue<PacketCacheExpiry>                  packetCacheExpiries = new ArrayDeque<>();

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
        final long expiresAt = System.nanoTime() + packetCacheLifetime;
        final PacketCacheExpiry expiry = new PacketCacheExpiry(new WeakReference<>(packetCache), expiresAt);
        packetCachesToClear.put(packetCache, expiry);
        packetCacheExpiries.add(expiry);
    }

    private static void clearExpiredPacketCaches()
    {
        if (packetCachesToClear.isEmpty())
        {
            packetCacheExpiries.clear();
            return;
        }

        final long now = System.nanoTime();
        while (!packetCacheExpiries.isEmpty() && now - packetCacheExpiries.peek().expiresAt() >= 0)
        {
            final PacketCacheExpiry expiry = packetCacheExpiries.poll();
            final IChunkPacketCache packetCache = expiry.packetCache().get();
            if (packetCache != null && packetCachesToClear.get(packetCache) == expiry)
            {
                packetCache.clearCachedPacket();
                packetCachesToClear.remove(packetCache);
            }
        }
    }

    /**
     * Expiry of a cached packet, queued in order of expiry. The map holds the current one of each chunk, an older one
     * is skipped, as is one whose chunk was collected in the meantime.
     */
    private record PacketCacheExpiry(WeakReference<IChunkPacketCache> packetCache, long expiresAt)
    {
    }
}
