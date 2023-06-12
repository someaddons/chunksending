package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.IChunksendingPlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerChunkSending extends PlayerEntity implements IChunksendingPlayer
{
    @Shadow
    public  ServerPlayNetHandler connection;
    @Shadow
    private boolean              disconnected;

    @Unique
    private Map<ChunkPos, List<IPacket<?>>> chunksToSend = new HashMap<>();

    public ServerPlayerChunkSending(World p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_)
    {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "trackChunk", at = @At("HEAD"), cancellable = true)
    private void chunksending$trackChunk(
      final ChunkPos pos,
      final IPacket<?> p_213844_2_,
      final IPacket<?> p_213844_3_,
      final CallbackInfo ci)
    {
        ci.cancel();

        List<IPacket<?>> packetList = chunksToSend.get(pos);
        if (packetList == null)
        {
            packetList = new ArrayList<>();
            chunksToSend.put(pos, packetList);
        }
        packetList.add(p_213844_2_);
        packetList.add(p_213844_3_);
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

        final List<Map.Entry<ChunkPos, List<IPacket<?>>>> packets = new ArrayList<>(chunksToSend.entrySet());
        packets.sort(Comparator.comparingDouble(
          e -> e.getKey().getWorldPosition().distSqr(blockPosition())
        ));

        for (int i = 0; i < packets.size() && i < ChunkSending.config.getCommonConfig().maxChunksPerTick; i++)
        {
            final Map.Entry<ChunkPos, List<IPacket<?>>> entry = packets.get(i);
            for (final IPacket packet : entry.getValue())
            {
                connection.send(packet);
            }
            chunksToSend.remove(entry.getKey());
        }
    }

    @Override
    public boolean attachToPending(final ChunkPos pos, final IPacket<?> packet)
    {
        final List<IPacket<?>> packetList = chunksToSend.get(pos);

        if (packetList == null)
        {
            return false;
        }

        packetList.add(packet);
        return true;
    }
}
