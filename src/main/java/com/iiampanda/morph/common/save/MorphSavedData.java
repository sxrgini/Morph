package com.iiampanda.morph.common.save;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphSavedData extends SavedData
{
    private static final String NAME = "morph_players";

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public static MorphSavedData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(MorphSavedData::new, MorphSavedData::load, null), NAME);
    }

    public PlayerData get(UUID id)
    {
        setDirty();
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    public static MorphSavedData load(CompoundTag tag, HolderLookup.Provider provider)
    {
        MorphSavedData data = new MorphSavedData();
        CompoundTag list = tag.getCompound("players");
        for(String key : list.getAllKeys())
        {
            try
            {
                PlayerData pd = new PlayerData();
                pd.read(list.getCompound(key));
                data.players.put(UUID.fromString(key), pd);
            }
            catch(IllegalArgumentException ignored) {}
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        CompoundTag list = new CompoundTag();
        for(Map.Entry<UUID, PlayerData> e : players.entrySet())
        {
            list.put(e.getKey().toString(), e.getValue().write());
        }
        tag.put("players", list);
        return tag;
    }
}
