package com.iiampanda.morph.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for the bits of CompoundTag that were directly accessible (tagMap) in 1.16.5.
 */
public final class NbtUtil
{
    private NbtUtil() {}

    public static List<String> keys(CompoundTag tag)
    {
        return new ArrayList<>(tag.getAllKeys());
    }

    /** Shallow put-all (replaces existing keys, unlike CompoundTag.merge which merges nested compounds). */
    public static void putAll(CompoundTag target, CompoundTag source)
    {
        for(String key : source.getAllKeys())
        {
            Tag t = source.get(key);
            if(t != null)
            {
                target.put(key, t);
            }
        }
    }

    public static void clear(CompoundTag tag)
    {
        for(String key : keys(tag))
        {
            tag.remove(key);
        }
    }

    public static void removeEmptyCompoundTags(CompoundTag tag)
    {
        for(String key : keys(tag))
        {
            Tag t = tag.get(key);
            if(t instanceof CompoundTag compound)
            {
                removeEmptyCompoundTags(compound);
                if(compound.isEmpty())
                {
                    tag.remove(key);
                }
            }
        }
    }
}
