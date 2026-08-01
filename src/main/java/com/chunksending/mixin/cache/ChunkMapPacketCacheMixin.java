package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.event.EventHandler;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkMap.class)
public class ChunkMapPacketCacheMixin
{
    @Shadow
    @Final
    private ServerLevel level;

    @Inject(method = "playerLoadedChunk", at = @At(value = "HEAD"))
    private void chunksending$prioCached(
        final ServerPlayer player,
        final MutableObject<ClientboundLevelChunkWithLightPacket> packetMutableObject,
        final LevelChunk chunk,
        final CallbackInfo ci)
    {
        if (chunksending$shouldUseCache(player, chunk) && packetMutableObject.getValue() == null && chunk instanceof IChunkPacketCache packetCache)
        {
            final ClientboundLevelChunkWithLightPacket packet = packetCache.getCachedPacket();
            if (packet != null)
            {
                packetMutableObject.setValue(packet);
            }
        }
    }

    @Inject(method = "playerLoadedChunk", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/mutable/MutableObject;setValue(Ljava/lang/Object;)V", shift = At.Shift.AFTER, remap = false))
    private void chunksending$cachePacket(
        final ServerPlayer player,
        final MutableObject<ClientboundLevelChunkWithLightPacket> mutableObject,
        final LevelChunk chunk,
        final CallbackInfo ci)
    {
        if (chunksending$shouldUseCache(player, chunk) && chunk instanceof IChunkPacketCache packetCache)
        {
            final ClientboundLevelChunkWithLightPacket packet = mutableObject.getValue();
            if (packet != null)
            {
                packetCache.setCachedPacket(packet);
                EventHandler.addToClear(packetCache);
            }
        }
    }

    @Inject(method = "resendBiomesForChunks", at = @At("HEAD"))
    private void chunksending$clearBeforeBiomeResend(final List<ChunkAccess> chunks, final CallbackInfo ci)
    {
        for (final ChunkAccess chunk : chunks)
        {
            final LevelChunk levelChunk = chunk instanceof LevelChunk fullChunk ? fullChunk : level.getChunk(chunk.getPos().x, chunk.getPos().z);

            if (levelChunk instanceof IChunkPacketCache packetCache)
            {
                packetCache.clearCachedPacket();
            }
        }
    }

    @Unique
    private boolean chunksending$shouldUseCache(final ServerPlayer player, final LevelChunk chunk)
    {
        return level.getServer().isSameThread() && player instanceof IChunksendingPlayer;
    }
}
