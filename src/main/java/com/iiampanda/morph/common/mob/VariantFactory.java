package com.iiampanda.morph.common.mob;

import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.regex.Pattern;

public final class VariantFactory
{
    private VariantFactory() {}

    public static boolean isMorphable(LivingEntity living)
    {
        if(living instanceof Player || !living.isAlive() && false)
        {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        for(Pattern p : Morph.configServer.disabledMobsID)
        {
            if(p.matcher(id.toString()).matches())
            {
                return false;
            }
        }
        MobProfile profile = MobProfileManager.getOrCreate(living);
        return profile.canMorph();
    }

    @Nullable
    public static MorphVariant create(LivingEntity living)
    {
        if(!isMorphable(living))
        {
            return null;
        }
        MobProfile profile = MobProfileManager.getOrCreate(living);
        CompoundTag full = new CompoundTag();
        living.saveWithoutId(full);
        CompoundTag nbt = new CompoundTag();
        if(profile.variantKeys != null)
        {
            for(String key : profile.variantKeys)
            {
                Tag t = full.get(key);
                if(t != null)
                {
                    nbt.put(key, t.copy());
                }
            }
        }
        boolean baby = living.isBaby() && profile.canBeBaby();
        return new MorphVariant(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()), nbt, baby);
    }

    public static MorphVariant basic(MorphVariant v)
    {
        return new MorphVariant(v.type, new CompoundTag(), false);
    }
}
