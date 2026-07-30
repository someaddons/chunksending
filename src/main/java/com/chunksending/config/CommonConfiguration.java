package com.chunksending.config;

import com.chunksending.ChunkSending;
import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public int maxChunksPerTick = 15;
    public boolean debugLogging = false;

    public CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry = new JsonObject();
        entry.addProperty("desc:", "Maximum amount of chunks sent per tick to a player");
        entry.addProperty("maxChunksPerTick", maxChunksPerTick);
        root.add("maxChunksPerTick", entry);

        final JsonObject entry23 = new JsonObject();
        entry23.addProperty("desc:", "Enable debug logging to show the amount of chunks sent/queued");
        entry23.addProperty("debugLogging", debugLogging);
        root.add("debugLogging", entry23);
        root.addProperty("version", 1);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            ChunkSending.LOGGER.error("Config file was empty!");
            return;
        }

        maxChunksPerTick = Math.max(1 ,data.get("maxChunksPerTick").getAsJsonObject().get("maxChunksPerTick").getAsInt());
        debugLogging = data.get("debugLogging").getAsJsonObject().get("debugLogging").getAsBoolean();
        if (!data.has("version") && maxChunksPerTick == 5)
        {
            maxChunksPerTick = 15;
            ChunkSending.config.save();
        }
    }
}
