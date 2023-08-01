package com.chunksending;

import com.chunksending.config.CommonConfiguration;
import com.cupboard.config.CupboardConfig;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
public class ChunkSending implements ModInitializer
{
    public static final String                              MODID  = "chunksending";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MODID, new CommonConfiguration());
    public static Random rand = new Random();

    public ChunkSending()
    {
    }

    @Override
    public void onInitialize()
    {
        config.load();
        LOGGER.info(MODID + " mod initialized");
    }
}
