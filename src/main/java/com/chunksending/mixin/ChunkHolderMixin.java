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

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin
{
    @Shadow
    @Final
    private ChunkHolder.PlayerProvider playerProvider;

    @Shadow
    @Final
    private ChunkPos pos;

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void chunksending$onBroadCastChanges(final Packet<?> packet, final boolean p_140065_, final CallbackInfo ci)
    {
        for (final ServerPlayer player : this.playerProvider.getPlayers(this.pos, p_140065_))
        {
            if (!((IChunksendingPlayer) player).attachToPending(pos, packet))
            {
                player.connection.send(packet);
            }
        }
        ci.cancel();
    }
}
