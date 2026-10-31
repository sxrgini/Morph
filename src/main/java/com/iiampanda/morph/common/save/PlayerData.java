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
        return tag;
    }

    public void read(CompoundTag tag)
    {
        morphs.clear();
        favourites.clear();
        purchased.clear();
        enabledAbilities.clear();
        current = null;
        for(Tag t : tag.getListOrEmpty("morphs"))
        {
            if(!(t instanceof CompoundTag ct)) continue;
            MorphVariant v = MorphVariant.read(ct);
            if(v != null)
            {
                morphs.add(v);
            }
        }
        for(Tag t : tag.getListOrEmpty("favourites"))
        {
            favourites.add(t.asString().orElse(""));
        }
        CompoundTag up = tag.getCompoundOrEmpty("purchased");
        for(String k : up.keySet())
        {
            purchased.put(k, up.getIntOr(k, 0));
        }
        for(Tag t : tag.getListOrEmpty("enabledAbilities"))
        {
            enabledAbilities.add(t.asString().orElse(""));
        }
        biomass = tag.getDoubleOr("biomass", 0D);
        if(tag.contains("current"))
        {
            current = MorphVariant.read(tag.getCompoundOrEmpty("current"));
        }
    }
}
