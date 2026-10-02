package com.chunksending.event;

import com.chunksending.chunk.IChunkPacketCache;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.config.CommonConfiguration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EventHandler
{
    private static final class PacketCacheEntry
    {
        private final WeakReference<IChunkPacketCache> packetCache;
        private final int                              hashCode;
        private final long                             expiresAt;

        PacketCacheEntry(IChunkPacketCache cache, long expiresAt)
        {
            this.packetCache = new WeakReference<>(cache);
            this.hashCode = System.identityHashCode(cache);
            this.expiresAt = expiresAt;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }

            if (!(obj instanceof PacketCacheEntry other))
            {
                return false;
            }

            final IChunkPacketCache a = packetCache.get();
            final IChunkPacketCache b = other.packetCache.get();

            return a != null && a == b;
        }
    }

    private static final long          packetCacheLifetime = TimeUnit.MINUTES.toNanos(5);
    private static final LinkedHashSet<PacketCacheEntry> packetCachesToClear = new LinkedHashSet<>();

    public static int maxChunksPerPlayer = 15;

    @SubscribeEvent()
    public static void onServerTick(ServerTickEvent.Pre tickEvent)
    {
        clearExpiredPacketCaches();

        int pendingPlayers = 0;
        int pendingChunks = 0;

        for (final ServerPlayer player : tickEvent.getServer().getPlayerList().getPlayers())
        {
            if (player.connection.chunkSender instanceof IChunksendingPlayer chunksendingPlayer)
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
        final PacketCacheEntry entry = new PacketCacheEntry(packetCache, System.nanoTime() + packetCacheLifetime);
        packetCachesToClear.remove(entry);
        packetCachesToClear.add(entry);
    }

    private static void clearExpiredPacketCaches()
    {
        if (packetCachesToClear.isEmpty())
        {
            return;
        }

        final long now = System.nanoTime();
        final Iterator<PacketCacheEntry> iterator = packetCachesToClear.iterator();

        while (iterator.hasNext())
        {
            final PacketCacheEntry entry = iterator.next();
            final IChunkPacketCache packetCache = entry.packetCache.get();
            if (packetCache == null)
            {
                iterator.remove();
            }
            else if (now - entry.expiresAt >= 0)
            {
                packetCache.clearCachedPacket();
                iterator.remove();
            }
            else
            {
                break;
            }
        }
    }
}
