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

import java.util.Comparator;
import java.util.function.ToIntFunction;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin implements IChunksendingPlayer
{
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
        if ((!memoryConnection || CommonConfiguration.config.getCommonConfig().enableSinglePlayer))
        {
            final int amount = (player.level().getServer().isDedicatedServer() ? 1 : 3) * EventHandler.maxChunksPerPlayer;
            desiredChunksPerTick = Math.min(desiredChunksPerTick, amount);
        }
    }

    @ModifyConstant(method = "onChunkBatchReceivedByClient", constant = @Constant(intValue = 10))
    private int adjustMaxUnacknowledgeBatches(final int constant)
    {
        return CommonConfiguration.config.getCommonConfig().maxUnacknowledgedChunkBatches;
    }

    @Unique
    private static int chunksSend = 0;

    @Inject(method = "sendChunk", at = @At("HEAD"))
    private static void incChunKCounter(
        final ServerGamePacketListenerImpl serverGamePacketListenerImpl,
        final ServerLevel serverLevel,
        final LevelChunk levelChunk,
        final CallbackInfo ci)
    {
        chunksSend++;
    }

    @Inject(method = "sendNextChunks", at = @At("HEAD"))
    private void resetChunkCounter(final ServerPlayer serverPlayer, final CallbackInfo ci)
    {
        chunksSend = 0;
    }

    @Inject(method = "sendNextChunks", at = @At(value = "RETURN"))
    private void logChunks(final ServerPlayer player, final CallbackInfo ci)
    {
        if (chunksSend > 0 && CommonConfiguration.config.getCommonConfig().debugLogging && (!memoryConnection || CommonConfiguration.config.getCommonConfig().enableSinglePlayer))
        {
            final int amount = (player.level().getServer().isDedicatedServer() ? 1 : 3) * EventHandler.maxChunksPerPlayer;
            ChunkSending.LOGGER.info(
                "Sent: " + chunksSend + " packets to " + player.getDisplayName().getString() + ", in queue:" + pendingChunks.size() + " maximum possible to send:" + amount
                    + " desiredChunksPerTick:" + desiredChunksPerTick + " unacknowledgedChunkBatches:" + unacknowledgedBatches + " batchQuota:" + batchQuota);
        }
    }

    @Redirect(method = "collectChunksToSend", at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingInt(Ljava/util/function/ToIntFunction;)Ljava/util/Comparator;"))
    private Comparator ajustSorting(final ToIntFunction keyExtractor, ChunkMap p_296053_, ChunkPos playerChunkPos)
    {
        if (!CommonConfiguration.config.getCommonConfig().prioritizeDirection)
        {
            return Comparator.comparingInt(keyExtractor);
        }

        return Comparator.comparingInt((Object unknown) ->
        {
            final long chunk;
            if (unknown instanceof Long value)
            {
                chunk = value;
            }
            else if (unknown instanceof LevelChunk levelChunk)
            {
                chunk = levelChunk.getPos().toLong();
            }
            else
            {
                throw new IllegalArgumentException(
                    "Unexpected type in chunk comparator: " + unknown.getClass()
                );
            }

            final int chunkDistance = Math.max(
                Math.abs(ChunkPos.getX(chunk) - playerChunkPos.x),
                Math.abs(ChunkPos.getZ(chunk) - playerChunkPos.z)
            );

            return chunkDistance <= 1 ? chunkDistance : 100;
        }).thenComparingInt(keyExtractor);
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
        if ((!memoryConnection || CommonConfiguration.config.getCommonConfig().enableSinglePlayer))
        {
            desiredChunksPerTick *= CommonConfiguration.config.getCommonConfig().desiredChunksPerTickModifier;
        }
    }
}
