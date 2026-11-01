package com.iiampanda.morph.common.mob;

import com.google.gson.annotations.SerializedName;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.TraitHandler;

import java.util.ArrayList;
import java.util.List;

public class MobProfile
{
    public String forEntity;

    public Boolean autoDetected;

    @SerializedName("_comment")
    public String comment;

    public List<String> variantKeys = new ArrayList<>();

    public Boolean allowBaby;

    public Boolean allowMorph;

    public Double biomassMultiplier;

    public Integer hotbarSlots;

    public Boolean specialsHandled;

    public Integer defaultsVersion;

    public List<Trait<?>> traits = new ArrayList<>();

    public boolean canMorph()
    {
        return allowMorph == null || allowMorph;
    }

    public boolean canBeBaby()
    {
        return allowBaby == null || allowBaby;
    }

    public ArrayList<Trait<?>> instantiateTraits()
    {
        ArrayList<Trait<?>> list = new ArrayList<>();
        if(traits != null)
        {
            for(Trait<?> trait : traits)
            {
                if(trait != null)
                {
                    list.add(TraitHandler.copy(trait));
                }
            }
        }
        return list;
    }
}
