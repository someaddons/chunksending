package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkMap.class)
public class ChunkMapPacketCacheMixin
{
    @Shadow
    @Final
    public ServerLevel level;

    @Inject(method = "resendBiomesForChunks", at = @At("HEAD"))
    private void chunksending$clearBeforeBiomeResend(final List<ChunkAccess> chunks, final CallbackInfo ci)
    {
        for (final ChunkAccess chunk : chunks)
        {
            final LevelChunk levelChunk = chunk instanceof LevelChunk fullChunk ? fullChunk : level.getChunk(chunk.getPos().x(), chunk.getPos().z());

            if (levelChunk instanceof IChunkPacketCache packetCache)
            {
                packetCache.clearCachedPacket();
            }
        }
    }
}
