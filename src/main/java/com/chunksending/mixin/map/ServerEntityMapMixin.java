package com.chunksending.mixin.map;

import com.chunksending.config.CommonConfiguration;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
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
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void checkType(ServerLevel level, Entity entity, int updateInterval, boolean trackDelta, ServerEntity.Synchronizer synchronizer, final CallbackInfo ci)
    {
        if (entity instanceof ItemFrame)
        {
            trackedPlayers = new ArrayList<>();
        }
    }

    @ModifyConstant(method = "sendChanges", constant = @Constant(classValue = ItemFrame.class, ordinal = 0), require = 1)
    private boolean chunksending$disableVanillaMapUpdates(final Object reference, final Class<?> targetClass)
    {
        return false;
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
                if (nearbyUpdate && !allUpdate && trackedPlayers.isEmpty())
                {
                    this.sendDirtyEntityData();
                    return;
                }

                final ItemStack itemstack = itemframe.getItem();
                MapItemSavedData mapitemsaveddata = MapItem.getSavedData(itemstack, this.level);
                if (mapitemsaveddata != null)
                {
                    MapId mapid = itemstack.get(DataComponents.MAP_ID);
                    for (ServerPlayer serverplayer : allUpdate ? this.level.players() : trackedPlayers)
                    {
                        mapitemsaveddata.tickCarriedBy(serverplayer, itemstack, itemframe);
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
