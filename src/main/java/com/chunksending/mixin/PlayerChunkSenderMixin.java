package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.IBatchedUpdateSender;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin implements IBatchedUpdateSender
{
    @Shadow
    @Final
    @Mutable
    private static float                          START_CHUNKS_PER_TICK = ChunkSending.config.getCommonConfig().maxChunksPerTick;
    @Shadow
    @Final
    @Mutable
    public static  float                          MAX_CHUNKS_PER_TICK   = ChunkSending.config.getCommonConfig().maxChunksPerTick * 2;
    @Shadow
    private        float                          desiredChunksPerTick  = ChunkSending.config.getCommonConfig().maxChunksPerTick;
    @Unique
    private        Map<ChunkPos, List<Packet<?>>> packetsToSend         = new HashMap<>();

    @Override
    public boolean attachToPending(final ChunkPos pos, final Packet<?> packet, final Player player)
    {
        List<Packet<?>> packetList = packetsToSend.get(pos);

        if (packetList == null)
        {
            packetList = new ArrayList<>();
            packetsToSend.put(pos, packetList);
        }

        packetList.add(packet);

        if (ChunkSending.config.getCommonConfig().debugLogging && packetsToSend.size() > 100)
        {
            if (packetsToSend.size() < 1000)
            {
                ChunkSending.LOGGER.info("Attached over: " + packetsToSend.size() + " packets" + player.getDisplayName().getString());
            }
            else
            {
                ChunkSending.LOGGER.info("Attached over: " + packetsToSend.size() + " packets player:" + player.getDisplayName().getString()+ " class:"+player.getClass() + " this:"+this, new RuntimeException());

            }
        }

        if (packetsToSend.size() > 2000)
        {
            packetsToSend.clear();
            return false;
        }

        return true;
    }

    @Override
    public void reset()
    {
        packetsToSend.clear();
    }

    @Override
    public void tick(final ServerPlayer player)
    {
        if (packetsToSend.isEmpty())
        {
            return;
        }

        final List<Map.Entry<ChunkPos, List<Packet<?>>>> packets = new ArrayList<>(packetsToSend.entrySet());
        packets.sort(Comparator.comparingDouble(
          e -> e.getKey().getMiddleBlockPosition(player.getBlockY()).distSqr(player.blockPosition())
        ));

        final int max = ChunkSending.config.getCommonConfig().maxChunksPerTick + packets.size() / 10;

        int count = 0;
        for (int i = 0; i < packets.size() && i < max; i++)
        {
            final Map.Entry<ChunkPos, List<Packet<?>>> entry = packets.get(i);
            for (final Packet packet : entry.getValue())
            {
                count++;
                player.connection.send(packet);
            }
            packetsToSend.remove(entry.getKey());
        }

        if (ChunkSending.config.getCommonConfig().debugLogging)
        {
            ChunkSending.LOGGER.info("Sent: " + count + " packets to " + player.getDisplayName().getString() + ", still in queue:" + packetsToSend.size());
        }
    }
}
