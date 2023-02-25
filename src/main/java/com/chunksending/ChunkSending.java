package com.chunksending;

import com.chunksending.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChunkSending.MODID)
public class ChunkSending
{
    public static final String MODID = "chunksending";
    public static final Logger LOGGER = LogManager.getLogger();
    public static Configuration config = new Configuration();
    public static Random rand = new Random();

    public ChunkSending()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }


    private void setup(final FMLCommonSetupEvent event)
    {
        config.load();
        LOGGER.info(MODID + " mod initialized");
    }
}
