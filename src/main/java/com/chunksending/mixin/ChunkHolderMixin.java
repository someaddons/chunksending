package com.chunksending.mixin;

import com.chunksending.IChunksendingPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin
{
    @Shadow
    @Final
    private ChunkHolder.PlayerProvider playerProvider;

    @Shadow
    @Final
    ChunkPos pos;

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void chunksending$onBroadCastChanges(
      final List<ServerPlayer> list,
      final Packet<?> packet,
      final CallbackInfo ci)
    {
        for (final ServerPlayer player : list)
        {
            if (!((IChunksendingPlayer) player).attachToPending(pos, packet))
            {
                player.connection.send(packet);
            }
        }
        ci.cancel();
    }
}
