package com.chunksending.config;

import com.cupboard.config.CupboardConfig;
import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public static CupboardConfig<CommonConfiguration> config = new CupboardConfig("chunksending", new CommonConfiguration());

    public int     maxChunksPerTick                 = 15;
    public int     maxChunksPerTickAll              = 80;
    public int     maxUnacknowledgedChunkBatches    = 5;
    public float   desiredChunksPerTickModifier     = 0.8f;
    public boolean prioritizeDirection              = true;
    public boolean cacheChunkPackets                = true;
    public boolean enableSinglePlayer               = true;
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

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc1:",
            "Maximum amount of chunks sent per tick to a player, note that this is a hard upper cap and clients do request their own desired rate. Default: 15 (x3 in singleplayer)");
        entry2.addProperty("maxChunksPerTick", maxChunksPerTick);
        entry2.addProperty("desc2:",
            "Maximum amount of chunks sent per tick globally for all players(equally split, minimum 1 per tick). Reduces the network impact of multiple players joining at once. Default: 80");
        entry2.addProperty("maxChunksPerTickAll", maxChunksPerTickAll);
        entry2.addProperty("desc3:",
            "Maximum amount of chunk batches which are sent without waiting for the client to confirm their arrival, lower values help slow connections to not get overwhelmed on login. Default: 5, Vanilla:10");
        entry2.addProperty("maxUnacknowledgedChunkBatches", maxUnacknowledgedChunkBatches);
        entry2.addProperty("desc4:",
            "Clients report how many chunks they desire to receive each tick, this setting allows multiplying it by a factor to reduce or increase the desired, lowering it reduces relative network bandwidth spent on chunks. Default: 0.8, Vanilla: 1.0, minimum 0.1");
        entry2.addProperty("desiredChunksPerTickModifier", desiredChunksPerTickModifier);
        entry2.addProperty("desc5:", "Enables chunks to prioritize loading in the direction the player is facing. Default: true");
        entry2.addProperty("prioritizeDirection", prioritizeDirection);
        entry2.addProperty("desc6:",
            "Enables chunk packets to be re-used to send to multiple players instead of creating new ones. Especially useful for many players on a server. Disable if you're having mod compat issues,  Default: true");
        entry2.addProperty("cacheChunkPackets", cacheChunkPackets);
        entry2.addProperty("desc7:",
            "Enables chunk batching in singleplayer, allowing weaker computers to struggle less when changing places. Default: true, Vanilla:false");
        entry2.addProperty("enableSinglePlayer", enableSinglePlayer);
        root.add("chunkBatching", entry2);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Settings for Item Frames containing maps sending updates to players. Default: enabled = true, set to false to disable this feature.");
        entry5.addProperty("enabled", itemFrameMapImprovements);
        entry5.addProperty("desc2:",
            "Sets the update interval at which itemframes send map updates to ALL players in a dimension so reducing the update rate does reduce network traffic especially when a lot of itemframes with maps exist. Default: 100 Ticks, Vanilla: 10 Ticks");
        entry5.addProperty("itemFrameMapUpdateInterval", itemFrameMapUpdateAllInterval);
        entry5.addProperty("desc3:", "Sets the update interval at which itemframes send map updates to nearby players. Default: 20 Ticks, Vanilla: 10 Ticks");
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
        maxChunksPerTick = Math.max(1, data.get("chunkBatching").getAsJsonObject().get("maxChunksPerTick").getAsInt());
        debugLogging = data.get("debugLogging").getAsJsonObject().get("debugLogging").getAsBoolean();
        if (!data.has("version") && maxChunksPerTick == 5)
        {
            maxChunksPerTick = 15;
            config.save();
        }
        maxChunksPerTickAll = Math.max(1, data.get("chunkBatching").getAsJsonObject().get("maxChunksPerTickAll").getAsInt());
        prioritizeDirection = data.get("chunkBatching").getAsJsonObject().get("prioritizeDirection").getAsBoolean();
        cacheChunkPackets = data.get("chunkBatching").getAsJsonObject().get("cacheChunkPackets").getAsBoolean();
        enableSinglePlayer = data.get("chunkBatching").getAsJsonObject().get("enableSinglePlayer").getAsBoolean();
        desiredChunksPerTickModifier = Math.max(0.1f, data.get("chunkBatching").getAsJsonObject().get("desiredChunksPerTickModifier").getAsFloat());
        maxUnacknowledgedChunkBatches = Math.max(1, data.get("chunkBatching").getAsJsonObject().get("maxUnacknowledgedChunkBatches").getAsInt());
        itemFrameMapImprovements = data.get("ItemFrameMaps").getAsJsonObject().get("enabled").getAsBoolean();
        itemFrameMapUpdateAllInterval = Math.max(1, data.get("ItemFrameMaps").getAsJsonObject().get("itemFrameMapUpdateInterval").getAsInt());
        itemFrameMapUpdateNearbyInterval = Math.max(1, data.get("ItemFrameMaps").getAsJsonObject().get("itemFrameMapUpdateNearbyInterval").getAsInt());
    }
}
