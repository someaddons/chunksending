package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkAccess.class)
public class ChunkAccessPacketCacheMixin
{
    @Inject(method = "setUnsaved", at = @At("HEAD"))
    private void chunksending$clearWhenDirty(final boolean unsaved, final CallbackInfo ci)
    {
        if (unsaved)
        {
            chunksending$clearCachedPacket();
        }
    }

    @Inject(method = "setHeightmap", at = @At("HEAD"))
    private void chunksending$clearBeforeHeightmapChange(
        final Heightmap.Types type,
        final long[] data,
        final CallbackInfo ci)
    {
        chunksending$clearCachedPacket();
    }

    @Inject(method = "setBlockEntityNbt", at = @At("HEAD"))
    private void chunksending$clearBeforePendingBlockEntityChange(final CompoundTag tag, final CallbackInfo ci)
    {
        chunksending$clearCachedPacket();
    }

    @Inject(method = "fillBiomesFromNoise", at = @At("HEAD"))
    private void chunksending$clearBeforeBiomeChange(
        final BiomeResolver resolver,
        final Climate.Sampler sampler,
        final CallbackInfo ci)
    {
        chunksending$clearCachedPacket();
    }

    @Unique
    private void chunksending$clearCachedPacket()
    {
        if ((Object)this instanceof IChunkPacketCache packetCache)
        {
            packetCache.clearCachedPacket();
        }
    }
}
