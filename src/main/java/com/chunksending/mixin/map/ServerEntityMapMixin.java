package com.chunksending.mixin.map;

import com.chunksending.config.CommonConfiguration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes getMapID lag, which unnecessarily gets called for all itemframe items
 */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMapMixin
{
    @Shadow
    @Final
    private Entity entity;

    @Shadow
    private int tickCount;

    @Shadow
    protected abstract void sendDirtyEntityData();

    @Shadow
    @Final
    private ServerLevel level;

    @ModifyConstant(method = "sendChanges", constant = @Constant(intValue = 0))
    private int skipVanilla(final int constant)
    {
        return 11;
    }

    @Inject(method = "sendChanges", at = @At("RETURN"))
    private void sendMap(final CallbackInfo ci)
    {
        if (entity instanceof ItemFrame itemframe)
        {
            final int tickCompare = this.tickCount + entity.getId();
            final boolean nearbyUpdate = tickCompare % CommonConfiguration.config.getCommonConfig().itemFrameMapUpdateNearbyInterval == 0;
            final boolean allUpdate = tickCompare % CommonConfiguration.config.getCommonConfig().itemFrameMapUpdateAllInterval == 0;
            if (nearbyUpdate || allUpdate)
            {
                final ItemStack itemstack = itemframe.getItem();
                MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, this.level);
                if (mapitemsaveddata != null)
                {
                    MapId mapid = itemstack.get(DataComponents.MAP_ID);
                    for (ServerPlayer serverplayer : (allUpdate || trackedPlayers == null) ? this.level.players() : trackedPlayers)
                    {
                        mapitemsaveddata.tickCarriedBy(serverplayer, itemstack);
                        Packet<?> packet = mapitemsaveddata.getUpdatePacket(mapid, serverplayer);
                        if (packet != null)
                        {
                            serverplayer.connection.send(packet);
                        }
                    }
                }

                this.sendDirtyEntityData();
            }
        }
    }

    @Unique
    private List<ServerPlayer> trackedPlayers = null;

    @Inject(method = "addPairing", at = @At("HEAD"))
    private void chunksending$onpair(final ServerPlayer player, final CallbackInfo ci)
    {
        if (entity instanceof ItemFrame)
        {
            if (trackedPlayers == null)
            {
                trackedPlayers = new ArrayList<>();
            }
            trackedPlayers.add(player);
        }
    }

    @Inject(method = "removePairing", at = @At("HEAD"))
    private void chunksending$onunpair(final ServerPlayer player, final CallbackInfo ci)
    {
        if (entity instanceof ItemFrame)
        {
            if (trackedPlayers != null)
            {
                trackedPlayers.remove(player);
            }
        }
    }
}
