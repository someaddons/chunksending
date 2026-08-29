package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public class LevelChunkPacketCacheMixin implements IChunkPacketCache
{
    @Unique
    private volatile ClientboundLevelChunkWithLightPacket cachedPacket = null;

    @Override
    public ClientboundLevelChunkWithLightPacket getCachedPacket()
    {
        return cachedPacket;
    }

    @Override
    public void setCachedPacket(final ClientboundLevelChunkWithLightPacket packet)
    {
        cachedPacket = packet;
    }

    @Override
    public void clearCachedPacket()
    {
        cachedPacket = null;
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void chunksending$clearBeforeBlockChange(
        final BlockPos pos, final BlockState state, final int flags, final CallbackInfoReturnable<BlockState> cir)
    {
        clearCachedPacket();
    }

    @Inject(method = "setBlockEntity", at = @At("HEAD"))
    private void chunksending$clearBeforeBlockEntityChange(final BlockEntity blockEntity, final CallbackInfo ci)
    {
        clearCachedPacket();
    }

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    private void chunksending$clearBeforeBlockEntityRemoval(final BlockPos pos, final CallbackInfo ci)
    {
        clearCachedPacket();
    }

    @Inject(method = "clearAllBlockEntities", at = @At("HEAD"))
    private void chunksending$clearBeforeBlockEntitiesAreCleared(final CallbackInfo ci)
    {
        clearCachedPacket();
    }

    @Inject(method = "replaceBiomes", at = @At("HEAD"))
    private void chunksending$clearBeforeBiomesAreReplaced(final FriendlyByteBuf buffer, final CallbackInfo ci)
    {
        clearCachedPacket();
    }

    @Inject(method = "setLoaded", at = @At("HEAD"))
    private void chunksending$clearWhenUnloaded(final boolean loaded, final CallbackInfo ci)
    {
        if (!loaded)
        {
            clearCachedPacket();
        }
    }
}
