package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.chunk.Data;
import com.chunksending.chunk.IChunksendingPlayer;
import com.chunksending.event.EventHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
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
    private boolean disconnected;

    @Unique
    private Map<ChunkPos, Data> chunksToSend = new HashMap<>();

    @Unique
    private ResourceKey<Level> lastDimension = null;

    @Unique
    private ChunkPos sending = null;

    public ServerPlayerChunkSending(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_)
    {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "untrackChunk", at = @At("HEAD"), cancellable = true)
    private void chunksending$untrackChunk(final ChunkPos pos, final CallbackInfo ci)
    {
        if (chunksending$resetDimensionIfNeeded())
        {
            return;
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

        if (chunksending$resetDimensionIfNeeded())
        {
            return;
        }

        final List<ChunkPos> positions = new ArrayList<>(chunksToSend.keySet());
        positions.sort(Comparator.comparingDouble(e -> e.getMiddleBlockPosition(getBlockY()).distSqr(blockPosition())));

        final int amount = (level().getServer().isDedicatedServer() ? 1 : 3) * EventHandler.maxChunksPerPlayer;
        int sentCount = 0;
        for (int i = 0; i < positions.size() && sentCount < amount; i++)
        {
            final ChunkPos chunkPos = positions.get(i);
            final Data data = chunksToSend.get(chunkPos);

            if (data.player() == (Object) this)
            {
                final LevelChunk chunk = level().getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk == null)
                {
                    if (!data.chunkMap().getPlayers(chunkPos, false).contains(data.player()))
                    {
                        chunksToSend.remove(chunkPos, data);
                    }
                    continue;
                }

                sentCount++;
                sending = chunkPos;
                data.chunkMap().playerLoadedChunk(data.player(), new MutableObject<>(), chunk);
                sending = null;

                chunksToSend.remove(chunkPos);
            }
            else
            {
                chunksToSend.remove(chunkPos);
            }
        }

        if (ChunkSending.config.getCommonConfig().debugLogging)
        {
            ChunkSending.LOGGER.info(
                "Sent: " + sentCount + " packets to " + getDisplayName().getString() + ", in queue:" + chunksToSend.size() + " maximum possible to send:" + amount);
        }
    }

    @Override
    public void attachToPending(final ChunkMap chunkMap, final ServerPlayer player, final ChunkPos pos)
    {
        chunksending$resetDimensionIfNeeded();
        chunksToSend.putIfAbsent(pos, new Data(chunkMap, player, pos));
    }

    @Override
    public boolean isSending(final ChunkPos pos)
    {
        return pos.equals(sending);
    }

    @Unique
    private boolean chunksending$resetDimensionIfNeeded()
    {
        final ResourceKey<Level> current = level().dimension();

        if (lastDimension == null)
        {
            lastDimension = current;
            return false;
        }

        if (lastDimension.equals(current))
        {
            return false;
        }

        lastDimension = current;
        chunksToSend.clear();
        sending = null;
        return true;
    }

    @Override
    public int chunksQueued()
    {
        return chunksToSend.size();
    }
}
