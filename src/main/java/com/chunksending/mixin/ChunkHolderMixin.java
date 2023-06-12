package com.chunksending.mixin;

import com.chunksending.IChunksendingPlayer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Collectors;

@Mixin(ChunkHolder.class)
public class ChunkHolderMixin
{
    @Shadow
    @Final
    private ChunkHolder.IPlayerProvider playerProvider;

    @Shadow
    @Final
    private ChunkPos pos;

    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void chunksending$onBroadCastChanges(final IPacket<?> packet, final boolean p_140065_, final CallbackInfo ci)
    {
        for (final ServerPlayerEntity player : this.playerProvider.getPlayers(this.pos, p_140065_).collect(Collectors.toList()))
        {
            if (!((IChunksendingPlayer) player).attachToPending(pos, packet))
            {
                player.connection.send(packet);
            }
        }
        ci.cancel();
    }
}
