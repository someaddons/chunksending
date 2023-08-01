package com.chunksending;

import com.chunksending.config.CommonConfiguration;
import com.cupboard.config.CupboardConfig;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChunkSending.MODID)
public class ChunkSending
{
    public static final String                              MODID            = "chunksending";
    public static final Logger                              LOGGER           = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config           = new CupboardConfig(MODID, new CommonConfiguration());
    public static       Random                              rand             = new Random();

    public ChunkSending()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info(MODID + " mod initialized");
    }
}
