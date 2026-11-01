package com.iiampanda.morph.common.save;

import com.iiampanda.morph.api.morph.MorphVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public class PlayerData
{
    public final List<MorphVariant> morphs = new ArrayList<>();
    public final Set<String> favourites = new LinkedHashSet<>();
    public final Map<String, Integer> purchased = new HashMap<>();

    public final Set<String> enabledAbilities = new HashSet<>();
    public double biomass;
    @Nullable public MorphVariant current;
    @Nullable public MorphVariant last;
    @Nullable public MorphVariant oneMorph;
    public String spawnDim = "";
    public long spawnPos;
    public boolean hasSpawn;
    public long oneNormalUntil;
    public long oneCooldownUntil;

    public boolean contains(MorphVariant v)
    {
        for(MorphVariant m : morphs)
        {
            if(m.equals(v))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isFavourite(MorphVariant v)
    {
        return favourites.contains(v.key());
    }

    public CompoundTag write()
    {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for(MorphVariant v : morphs)
        {
            list.add(v.write());
        }
        tag.put("morphs", list);
        ListTag favs = new ListTag();
        for(String f : favourites)
        {
            favs.add(StringTag.valueOf(f));
        }
        tag.put("favourites", favs);
        CompoundTag up = new CompoundTag();
        for(Map.Entry<String, Integer> e : purchased.entrySet())
        {
            up.putInt(e.getKey(), e.getValue());
        }
        tag.put("purchased", up);
        ListTag abilities = new ListTag();
        for(String a : enabledAbilities)
        {
            abilities.add(StringTag.valueOf(a));
        }
        tag.put("enabledAbilities", abilities);
        tag.putDouble("biomass", biomass);
        if(current != null)
        {
            tag.put("current", current.write());
        }
        if(last != null)
        {
            tag.put("last", last.write());
        }
        if(oneMorph != null)
        {
            tag.put("oneMorph", oneMorph.write());
        }
        if(oneNormalUntil != 0L)
        {
            tag.putLong("oneNormalUntil", oneNormalUntil);
        }
        if(oneCooldownUntil != 0L)
        {
            tag.putLong("oneCooldownUntil", oneCooldownUntil);
        }
        if(hasSpawn)
        {
            tag.putString("spawnDim", spawnDim);
            tag.putLong("spawnPos", spawnPos);
        }
        return tag;
    }

    public void read(CompoundTag tag)
    {
        morphs.clear();
        favourites.clear();
        purchased.clear();
        enabledAbilities.clear();
        current = null;
        last = null;
        oneMorph = null;
        hasSpawn = false;
        spawnDim = "";
        spawnPos = 0L;
        oneNormalUntil = 0L;
        oneCooldownUntil = 0L;
        for(Tag t : tag.getList("morphs", Tag.TAG_COMPOUND))
        {
            MorphVariant v = MorphVariant.read((CompoundTag)t);
            if(v != null)
            {
                morphs.add(v);
            }
        }
        for(Tag t : tag.getList("favourites", Tag.TAG_STRING))
        {
            favourites.add(t.getAsString());
        }
        CompoundTag up = tag.getCompound("purchased");
        for(String k : up.getAllKeys())
        {
            purchased.put(k, up.getInt(k));
        }
        for(Tag t : tag.getList("enabledAbilities", Tag.TAG_STRING))
        {
            enabledAbilities.add(t.getAsString());
        }
        biomass = tag.getDouble("biomass");
        if(tag.contains("current"))
        {
            current = MorphVariant.read(tag.getCompound("current"));
        }
        if(tag.contains("last"))
        {
            last = MorphVariant.read(tag.getCompound("last"));
        }
        if(tag.contains("oneMorph"))
        {
            oneMorph = MorphVariant.read(tag.getCompound("oneMorph"));
        }
        if(tag.contains("spawnDim"))
        {
            spawnDim = tag.getString("spawnDim");
            spawnPos = tag.getLong("spawnPos");
            hasSpawn = !spawnDim.isEmpty();
        }
        oneNormalUntil = tag.getLong("oneNormalUntil");
        oneCooldownUntil = tag.getLong("oneCooldownUntil");
    }
}
