package com.chunksending.mixin.cache;

import com.chunksending.chunk.IChunkPacketCache;
import com.chunksending.config.CommonConfiguration;
import com.chunksending.event.EventHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.BitSet;

@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderPacketCacheMixin
{
    @WrapOperation(method = "sendChunk", at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/lighting/LevelLightEngine;Ljava/util/BitSet;Ljava/util/BitSet;)Lnet/minecraft/network/protocol/game/ClientboundLevelChunkWithLightPacket;"))
    private static ClientboundLevelChunkWithLightPacket chunksending$prioCached(
        final LevelChunk chunk,
        final LevelLightEngine lightEngine,
        final BitSet bitSet1,
        final BitSet bitSet2,
        final Operation<ClientboundLevelChunkWithLightPacket> original, ServerGamePacketListenerImpl packetListener, ServerLevel p_294963_, LevelChunk p_295144_)
    {
        if (chunksending$shouldUseCache(packetListener.player, chunk) && chunk instanceof IChunkPacketCache packetCache)
        {
            final ClientboundLevelChunkWithLightPacket packet = packetCache.getCachedPacket();
            if (packet != null)
            {
                return packet;
            }
            ClientboundLevelChunkWithLightPacket originalPacket = original.call(chunk, lightEngine, bitSet1, bitSet2);
            packetCache.setCachedPacket(originalPacket);
            EventHandler.addToClear(packetCache);
            return originalPacket;
        }

        return original.call(chunk, lightEngine, bitSet1, bitSet2);
    }

    @Unique
    private static boolean chunksending$shouldUseCache(final ServerPlayer player, final LevelChunk chunk)
    {
        return player.level().getServer().isSameThread() && CommonConfiguration.config.getCommonConfig().cacheChunkPackets;
    }
}
