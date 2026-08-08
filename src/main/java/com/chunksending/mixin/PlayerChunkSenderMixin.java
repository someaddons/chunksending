package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.config.CommonConfiguration;
import com.chunksending.event.EventHandler;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin implements IChunksendingPlayer
{
    // TODO: Hook config for vanilla, load logging, recheck other config features
    @Shadow
    @Final
    private LongSet pendingChunks;

    @Override
    public int chunksQueued()
    {
        return pendingChunks.size();
    }

    @Shadow
    private float desiredChunksPerTick;

    @Shadow
    private int unacknowledgedBatches;

    @Shadow
    private float batchQuota;

    @Shadow
    @Final
    @Mutable
    private boolean memoryConnection;

    @Inject(method = "sendNextChunks", at = @At("HEAD"))
    private void adjustMaxSend(final ServerPlayer player, final CallbackInfo ci)
    {
        final int amount = (player.level().getServer().isDedicatedServer() ? 1 : 3) * EventHandler.maxChunksPerPlayer;
        desiredChunksPerTick = Math.min(desiredChunksPerTick, amount);
    }

    @ModifyConstant(method = "onChunkBatchReceivedByClient", constant = @Constant(intValue = 10))
    private int adjustMaxUnacknowledgeBatches(final int constant)
    {
        return CommonConfiguration.config.getCommonConfig().maxUnacknowledgedChunkBatches;
    }

    @Inject(method = "sendNextChunks", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void logChunks(
        final ServerPlayer player,
        final CallbackInfo ci,
        final float f,
        final ServerLevel serverlevel,
        final ChunkMap chunkmap,
        final List list,
        final ServerGamePacketListenerImpl servergamepacketlistenerimpl)
    {
        if (CommonConfiguration.config.getCommonConfig().debugLogging)
        {
            final int amount = (player.level().getServer().isDedicatedServer() ? 1 : 3) * EventHandler.maxChunksPerPlayer;
            ChunkSending.LOGGER.info(
                "Sent: " + list.size() + " packets to " + player.getDisplayName().getString() + ", in queue:" + pendingChunks.size() + " maximum possible to send:" + amount
                    + " desiredChunksPerTick:" + desiredChunksPerTick + " unacknowledgedChunkBatches:" + unacknowledgedBatches + " batchQuota:" + batchQuota);
        }
    }

    @Redirect(method = "collectChunksToSend", at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;", ordinal = 0))
    private Comparator<Long> ajustSorting(final ToIntFunction<? super Long> keyExtractor, ChunkMap p_296053_, ChunkPos playerChunkPos)
    {
        if (!CommonConfiguration.config.getCommonConfig().prioritizeDirection)
        {
            return Comparator.comparingInt(keyExtractor);
        }

        return Comparator.comparingInt((Long chunk) -> {
            final int chunkDistance = Math.max(
                Math.abs(ChunkPos.getX(chunk) - playerChunkPos.x),
                Math.abs(ChunkPos.getZ(chunk) - playerChunkPos.z)
            );

            return chunkDistance <= 1 ? chunkDistance : 100;
        }).thenComparing(p -> playerChunkPos.distanceSquared(p));
    }

    @Redirect(method = "sendNextChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos adjustPlayerPosition(final ServerPlayer player)
    {
        if (!CommonConfiguration.config.getCommonConfig().prioritizeDirection)
        {
            return player.chunkPosition();
        }

        final BlockPos playerPos = player.blockPosition();
        final Vec3 lookAngle = player.getLookAngle().multiply(1, 0, 1).normalize();
        return new ChunkPos(SectionPos.blockToSectionCoord((int) (playerPos.getX() + lookAngle.x * 16 * 3)),
            SectionPos.blockToSectionCoord((int) (playerPos.getZ() + lookAngle.z * 16 * 3)));
    }

    @Inject(method = "onChunkBatchReceivedByClient", at = @At("RETURN"))
    private void adjustDesiredChunksPerTick(final float p_294462_, final CallbackInfo ci)
    {
        desiredChunksPerTick *= CommonConfiguration.config.getCommonConfig().desiredChunksPerTickModifier;
    }

    @Unique
    private boolean originalMemoryConnection = false;

    @Inject(method = "collectChunksToSend", at = @At("HEAD"))
    private void batchSinglePlayer(final ChunkMap p_296053_, final ChunkPos p_295659_, final CallbackInfoReturnable<List<LevelChunk>> cir)
    {
        if (CommonConfiguration.config.getCommonConfig().enableSinglePlayer)
        {
            originalMemoryConnection = memoryConnection;
            memoryConnection = false;
        }
    }

    @Inject(method = "collectChunksToSend", at = @At("RETURN"))
    private void batchSinglePlayerRestore(final ChunkMap p_296053_, final ChunkPos p_295659_, final CallbackInfoReturnable<List<LevelChunk>> cir)
    {
        if (CommonConfiguration.config.getCommonConfig().enableSinglePlayer)
        {
            memoryConnection = originalMemoryConnection;
        }
    }
}
