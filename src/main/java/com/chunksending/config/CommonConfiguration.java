package com.chunksending.config;

import com.cupboard.config.CupboardConfig;
import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public static CupboardConfig<CommonConfiguration> config = new CupboardConfig("chunksending", new CommonConfiguration());

    public int     maxChunksPerTick                 = 15;
    public int     maxChunksPerTickAll              = 80;
    public boolean prioritizeDirection              = true;
    public boolean cacheChunkPackets                = true;
    public boolean debugLogging                     = false;
    public int     itemFrameMapUpdateAllInterval    = 100;
    public int     itemFrameMapUpdateNearbyInterval = 20;
    public boolean itemFrameMapImprovements         = true;

    public CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();
        root.addProperty("desc:",
            "This is the config for the chunk sending mod, it is mostly relevant for dedicated servers. A Tick in minecraft happens 20 times a second(20TPS), a chunk is a 16x16 block area that gets sent to the client.");

        final JsonObject entry = new JsonObject();
        entry.addProperty("desc:", "Maximum amount of chunks sent per tick to a player. Default: 15 (x3 in singleplayer)");
        entry.addProperty("maxChunksPerTick", maxChunksPerTick);
        root.add("maxChunksPerTick", entry);

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:",
            "Maximum amount of chunks sent per tick globally for all players(equally split, minimum 1 per tick). Reduces the network impact of multiple players joining at once. Default: 80");
        entry2.addProperty("maxChunksPerTickAll", maxChunksPerTickAll);
        root.add("maxChunksPerTickAll", entry2);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Enables chunks to prioritize loading in the direction the player is facing. Default: true");
        entry3.addProperty("prioritizeDirection", prioritizeDirection);
        root.add("prioritizeDirection", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:",
            "Enables chunk packets to be re-used to send to multiple players instead of creating new ones. Especially useful for many players on a server. Disable if you're having mod compat issues,  Default: true");
        entry4.addProperty("cacheChunkPackets", cacheChunkPackets);
        root.add("cacheChunkPackets", entry4);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Settings for Item Frames containing maps sending updates to players. Default: enabled = true, set to false to disable this feature.");
        entry5.addProperty("enabled", itemFrameMapImprovements);
        entry5.addProperty("desc2:",
            "Sets the update interval at which itemframes send map updates to ALL players in a dimension, so reducing the update rate does reduce network traffic especially when a lot of itemframes with maps exist. Default: 100 Ticks, Vanilla: 10 Ticks");
        entry5.addProperty("itemFrameMapUpdateInterval", itemFrameMapUpdateAllInterval);
        entry5.addProperty("descNearby:", "Sets the update interval at which itemframes send map updates to nearby players. Default: 20 Ticks, Vanilla: 10 Ticks");
        entry5.addProperty("itemFrameMapUpdateNearbyInterval", itemFrameMapUpdateNearbyInterval);
        root.add("ItemFrameMaps", entry5);

        final JsonObject entry23 = new JsonObject();
        entry23.addProperty("desc:", "Enable debug logging to show the amount of chunks sent/queued");
        entry23.addProperty("debugLogging", debugLogging);
        root.add("debugLogging", entry23);
        root.addProperty("version", 1);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        maxChunksPerTick = Math.max(1, data.get("maxChunksPerTick").getAsJsonObject().get("maxChunksPerTick").getAsInt());
        debugLogging = data.get("debugLogging").getAsJsonObject().get("debugLogging").getAsBoolean();
        if (!data.has("version") && maxChunksPerTick == 5)
        {
            maxChunksPerTick = 15;
            config.save();
        }
        maxChunksPerTickAll = Math.max(1, data.get("maxChunksPerTickAll").getAsJsonObject().get("maxChunksPerTickAll").getAsInt());
        prioritizeDirection = data.get("prioritizeDirection").getAsJsonObject().get("prioritizeDirection").getAsBoolean();
        cacheChunkPackets = data.get("cacheChunkPackets").getAsJsonObject().get("cacheChunkPackets").getAsBoolean();
        itemFrameMapImprovements = data.get("ItemFrameMaps").getAsJsonObject().get("enabled").getAsBoolean();
        itemFrameMapUpdateAllInterval = Math.max(1, data.get("ItemFrameMaps").getAsJsonObject().get("itemFrameMapUpdateInterval").getAsInt());
        itemFrameMapUpdateNearbyInterval = Math.max(1, data.get("ItemFrameMaps").getAsJsonObject().get("itemFrameMapUpdateNearbyInterval").getAsInt());
    }
}
