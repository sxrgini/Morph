package com.iiampanda.morph.common.mob;

import com.google.gson.annotations.SerializedName;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.TraitHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * What a mob type can do when a player morphs into it. Loaded from config/morph/mobs/&lt;namespace&gt;/&lt;path&gt;.json.
 * Files marked "autoDetected" were written by Morph after inspecting the mob; edit them freely, Morph never overwrites an existing file.
 */
public class MobProfile
{
    public String forEntity;

    /** true when Morph generated this file by inspecting the mob (mostly for modded mobs). */
    public Boolean autoDetected;

    /** Short note shown at the top of generated files. */
    @SerializedName("_comment")
    public String comment;

    /** NBT keys that make two mobs of the same type count as different variants (coat colour, type, size...). */
    public List<String> variantKeys = new ArrayList<>();

    /** Set to false to stop players from acquiring/using the baby form of this mob. */
    public Boolean allowBaby;

    /** Set to false to stop players from acquiring/using this mob entirely. */
    public Boolean allowMorph;

    /** Scales how much biomass this mob is worth in Default mode. */
    public Double biomassMultiplier;

    /** Morph adds a default special move (Warden, Wither, modded bosses...) once, then sets this to true. Delete the special ability from the file and leave this true to give a boss none. */
    public Boolean specialsHandled;

    /** Which build last brought this file up to date (see MobProfiler.migrate). */
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

    /** Fresh copies, so each morphing player owns its trait state. */
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
