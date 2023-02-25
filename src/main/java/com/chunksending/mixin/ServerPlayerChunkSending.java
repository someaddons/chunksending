package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChunkSending extends Player
{
    @Shadow
    public ServerGamePacketListenerImpl connection;
    @Shadow
    private boolean disconnected;

    @Unique
    private Map<ChunkPos, Packet<?>> chunksToSend = new HashMap<>();

    public ServerPlayerChunkSending(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_)
    {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "trackChunk", at = @At("HEAD"), cancellable = true)
    private void chunksending$trackChunk(ChunkPos pos, Packet<?> chunkPacket, CallbackInfo ci)
    {
        ci.cancel();

        chunksToSend.put(pos, chunkPacket);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void chunksending$update(CallbackInfo ci)
    {
        if (chunksToSend.isEmpty())
        {
            return;
        }

        if (disconnected)
        {
            chunksToSend.clear();
            return;
        }

        final List<Map.Entry<ChunkPos, Packet<?>>> packets = new ArrayList<>(chunksToSend.entrySet());
        packets.sort(Comparator.comparingDouble(
                e -> e.getKey().getMiddleBlockPosition(getBlockY()).distSqr(blockPosition())
        ));

        for (int i = 0; i < packets.size() && i < ChunkSending.config.getCommonConfig().maxChunksPerTick; i++)
        {
            final Map.Entry<ChunkPos, Packet<?>> entry = packets.get(i);
            connection.send(entry.getValue());
            chunksToSend.remove(entry.getKey());
        }
    }
}
