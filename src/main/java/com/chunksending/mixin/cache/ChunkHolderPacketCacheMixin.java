package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderPacketCacheMixin
{
    @Shadow
    private boolean hasChangedSections;
    @Shadow
    @Final
    private BitSet  skyChangedLightSectionFilter;
    @Shadow
    @Final
    private BitSet  blockChangedLightSectionFilter;

    @Shadow
    public abstract LevelChunk getTickingChunk();

    @Inject(method = "blockChanged", at = @At("RETURN"))
    private void chunksending$clearAfterBlockChanged(final BlockPos pos, final CallbackInfoReturnable<Boolean> cir)
    {
        if (hasChangedSections)
        {
            chunksending$clearCachedPacket();
        }
    }

    @Inject(method = "sectionLightChanged", at = @At("RETURN"))
    private void chunksending$clearAfterLightChanged(
        final LightLayer layer, final int chunkY, final CallbackInfoReturnable<Boolean> cir)
    {
        if (!skyChangedLightSectionFilter.isEmpty() || !blockChangedLightSectionFilter.isEmpty())
        {
            chunksending$clearCachedPacket();
        }
    }

    @Unique
    private void chunksending$clearCachedPacket()
    {
        final LevelChunk chunk = getTickingChunk();
        if (chunk instanceof IChunkPacketCache packetCache)
        {
            packetCache.clearCachedPacket();
        }
    }
}
