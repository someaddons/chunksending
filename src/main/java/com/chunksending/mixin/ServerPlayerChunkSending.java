package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.IChunksendingPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
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
public abstract class ServerPlayerChunkSending extends Player implements IChunksendingPlayer
{
    @Shadow
    public  ServerGamePacketListenerImpl connection;
    @Shadow
    private boolean                      disconnected;

    @Unique
    private Map<ChunkPos, List<Packet<?>>> chunksToSend = new HashMap<>();

    @Unique
    private ResourceKey<Level> lastDimension = null;

    public ServerPlayerChunkSending(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_)
    {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "trackChunk", at = @At("HEAD"), cancellable = true)
    private void chunksending$trackChunk(ChunkPos pos, Packet<?> chunkPacket, CallbackInfo ci)
    {
        ci.cancel();

        if (level() != null && lastDimension != level().dimension())
        {
            lastDimension = level().dimension();
            chunksToSend.clear();
        }

        List<Packet<?>> packetList = chunksToSend.get(pos);
        if (packetList == null)
        {
            packetList = new ArrayList<>();
            chunksToSend.put(pos, packetList);
        }
        packetList.add(chunkPacket);
    }

    @Inject(method = "untrackChunk", at = @At("HEAD"), cancellable = true)
    private void chunksending$untrackChunk(final ChunkPos pos, final CallbackInfo ci)
    {
        if (level() != null && lastDimension != level().dimension())
        {
            lastDimension = level().dimension();
            chunksToSend.clear();
        }

        if (chunksToSend.remove(pos) != null)
        {
            ci.cancel();
        }
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

        if (level() != null && lastDimension != level().dimension())
        {
            lastDimension = level().dimension();
            chunksToSend.clear();
            return;
        }

        final List<Map.Entry<ChunkPos, List<Packet<?>>>> packets = new ArrayList<>(chunksToSend.entrySet());
        packets.sort(Comparator.comparingDouble(
          e -> e.getKey().getMiddleBlockPosition(getBlockY()).distSqr(blockPosition())
        ));

        final int amount = ChunkSending.config.getCommonConfig().maxChunksPerTick;
        int i = 0;
        for (; i < packets.size() && i < amount; i++)
        {
            final Map.Entry<ChunkPos, List<Packet<?>>> entry = packets.get(i);
            for (final Packet packet : entry.getValue())
            {
                connection.send(packet);
            }
            chunksToSend.remove(entry.getKey());
        }

        if (ChunkSending.config.getCommonConfig().debugLogging)
        {
            ChunkSending.LOGGER.info("Sent: "+i+" packets to "+getDisplayName().getString()+", in queue:"+ chunksToSend.size());
        }
    }

    @Override
    public boolean attachToPending(final ChunkPos pos, final Packet<?> packet)
    {
        final List<Packet<?>> packetList = chunksToSend.get(pos);

        if (packetList == null)
        {
            return false;
        }

        packetList.add(packet);
        return true;
    }
}
