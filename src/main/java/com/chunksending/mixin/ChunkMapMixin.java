package com.chunksending.mixin;

import com.chunksending.chunk.IChunksendingPlayer;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin
{
    @Shadow @Final private ServerLevel level;

    @Shadow protected abstract void playerLoadedChunk(
        final ServerPlayer p_183761_,
        final MutableObject<ClientboundLevelChunkWithLightPacket> p_183762_,
        final LevelChunk p_183763_);

    @Inject(method = "playerLoadedChunk", at = @At("HEAD"), cancellable = true)
    private void chunkSending$defer(
        final ServerPlayer serverPlayer,
        final MutableObject<ClientboundLevelChunkWithLightPacket> packetMutableObject,
        final LevelChunk chunk,
        final CallbackInfo ci)
    {
        if (!level.getServer().isSameThread())
        {
            ci.cancel();
            level.getServer().execute(() -> playerLoadedChunk(serverPlayer, packetMutableObject, chunk));
            return;
        }

        if (serverPlayer instanceof IChunksendingPlayer chunkSendingPlayer && !chunkSendingPlayer.isSending(chunk.getPos()))
        {
            chunkSendingPlayer.attachToPending((ChunkMap)(Object)this, serverPlayer, chunk.getPos());
            ci.cancel();
        }
    }
}
