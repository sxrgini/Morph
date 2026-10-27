package com.iiampanda.morph.common.save;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MorphSavedData extends SavedData
{
    private static final Codec<MorphSavedData> CODEC = CompoundTag.CODEC.xmap(MorphSavedData::load, MorphSavedData::save);

    public static final SavedDataType<MorphSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("morph", "morph_players"), MorphSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public static MorphSavedData get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public PlayerData get(UUID id)
    {
        setDirty();
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    public static MorphSavedData load(CompoundTag tag)
    {
        MorphSavedData data = new MorphSavedData();
        CompoundTag list = tag.getCompoundOrEmpty("players");
        for(String key : list.keySet())
        {
            try
            {
                PlayerData pd = new PlayerData();
                pd.read(list.getCompoundOrEmpty(key));
                data.players.put(UUID.fromString(key), pd);
            }
            catch(IllegalArgumentException ignored) {}
        }
        return data;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        CompoundTag list = new CompoundTag();
        for(Map.Entry<UUID, PlayerData> e : players.entrySet())
        {
            list.put(e.getKey().toString(), e.getValue().write());
        }
        tag.put("players", list);
        return tag;
    }
}
