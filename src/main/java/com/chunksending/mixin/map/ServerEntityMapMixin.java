package com.chunksending.mixin.map;

import com.chunksending.config.CommonConfiguration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
    private void checkType(final ServerLevel p_8528_, final Entity entity, final int p_8530_, final boolean p_8531_, final Consumer p_8532_, final CallbackInfo ci)
    {
        if (entity instanceof ItemFrame)
        {
            trackedPlayers = new ArrayList<>();
        }
    }

    @ModifyVariable(method = "sendChanges", at = @At(value = "STORE", ordinal = 0))
    private Entity test(final Entity entity)
    {
        return null;
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
                    final CompoundTag compoundtag = itemstack.getTag();
                    final int id = compoundtag != null && compoundtag.contains("map", 99) ? compoundtag.getInt("map") : -1;

                    for (ServerPlayer serverplayer : allUpdate ? this.level.players() : trackedPlayers)
                    {
                        mapitemsaveddata.tickCarriedBy(serverplayer, itemstack);
                        Packet<?> packet = mapitemsaveddata.getUpdatePacket(id, serverplayer);
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
