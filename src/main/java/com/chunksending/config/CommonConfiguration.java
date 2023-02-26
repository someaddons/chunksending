package com.chunksending.config;

import com.chunksending.ChunkSending;
import com.google.gson.JsonObject;

public class CommonConfiguration
{
    public int maxChunksPerTick = 5;

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

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            ChunkSending.LOGGER.error("Config file was empty!");
            return;
        }

        maxChunksPerTick = data.get("maxChunksPerTick").getAsJsonObject().get("maxChunksPerTick").getAsInt();
    }
}
