package com.chunksending.config;

import com.chunksending.ChunkSending;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Configuration
{
    /**
     * Loaded everywhere, not synced
     */
    private final CommonConfiguration commonConfig = new CommonConfiguration();

    /**
     * Loaded clientside, not synced
     */
    // private final ClientConfiguration clientConfig;
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Builds configuration tree.
     */
    public Configuration()
    {
    }

    public void load()
    {
        final Path configPath = FabricLoader.getInstance().getConfigDir().normalize().resolve(ChunkSending.MODID + ".json");
        final File config = configPath.toFile();
        if (!config.exists())
        {
            ChunkSending.LOGGER.warn("Config not found, recreating default");
            save();
        } else
        {
            try
            {
                commonConfig.deserialize(gson.fromJson(Files.newBufferedReader(configPath), JsonObject.class));
            }
            catch (Exception e)
            {
                ChunkSending.LOGGER.error("Could not read config from:" + configPath, e);
                save();
            }
        }
    }

    public void save()
    {
        final Path configPath = FabricLoader.getInstance().getConfigDir().normalize().resolve(ChunkSending.MODID + ".json");
        try
        {
            final BufferedWriter writer = Files.newBufferedWriter(configPath);
            gson.toJson(commonConfig.serialize(), JsonObject.class, writer);
            writer.close();
        }
        catch (IOException e)
        {
            ChunkSending.LOGGER.error("Could not write config to:" + configPath, e);
        }
        load();
    }

    public CommonConfiguration getCommonConfig()
    {
        return commonConfig;
    }
}
