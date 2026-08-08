package com.chunksending;

import com.chunksending.event.EventHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChunkSending.MODID)
public class ChunkSending
{
    public static final String MODID  = "chunksending";
    public static final Logger LOGGER = LogManager.getLogger();
    public static       Random rand   = new Random();

    public ChunkSending(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.addListener(EventHandler::onServerTick);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info(MODID + " mod initialized");
    }
}
