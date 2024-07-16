package com.chunksending.mixin;

import com.chunksending.ChunkSending;
import com.chunksending.IBatchedUpdateSender;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChunkSending extends Player
{
    @Shadow
    public  ServerGamePacketListenerImpl connection;
    @Shadow
    private boolean                      disconnected;

    public ServerPlayerChunkSending(Level p_250508_, BlockPos p_250289_, float p_251702_, GameProfile p_252153_)
    {
        super(p_250508_, p_250289_, p_251702_, p_252153_);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void chunksending$update(CallbackInfo ci)
    {
        if (disconnected)
        {
            ((IBatchedUpdateSender) connection.chunkSender).reset();
            return;
        }

        ((IBatchedUpdateSender) connection.chunkSender).tick((ServerPlayer) (Object) this);
    }
}
