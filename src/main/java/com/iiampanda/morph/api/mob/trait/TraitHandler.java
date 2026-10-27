package com.iiampanda.morph.api.mob.trait;

import com.google.gson.*;
import com.iiampanda.morph.api.mob.trait.ability.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;

/** Registry of trait types plus the Gson (de)serialiser used for mob profiles and for syncing traits to clients. */
public final class TraitHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger("Morph/Traits");
    private static final LinkedHashMap<String, Class<? extends Trait>> TRAITS = new LinkedHashMap<>();

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().registerTypeHierarchyAdapter(Trait.class, new Adapter()).create();

    static
    {
        register("traitFallNegate", FallNegateTrait.class);
        register("traitFloat", FloatTrait.class);
        register("traitHostile", HostileTrait.class);
        register("traitImmunityExplosive", ExplosiveImmunityTrait.class);
        register("traitImmunityFire", FireImmunityTrait.class);
        register("traitImmunityMagic", MagicImmunityTrait.class);
        register("traitSink", SinkTrait.class);
        register("traitWaterSensitivity", WaterSensitivityTrait.class);
        register("traitUndead", UndeadTrait.class);
        register("traitEffectResistance", EffectResistanceTrait.class);
        register("traitImmunityDamageSource", DamageSourceImmunityTrait.class);
        register("traitIntimidate", IntimidateTrait.class);
        register("traitMoistSkin", MoistSkinTrait.class);
        register("traitStepHeight", StepHeightTrait.class);
        register("traitSwimmer", SwimmerTrait.class);
        register("traitSunburn", SunburnTrait.class);
        register("traitWaterBreather", WaterBreatherTrait.class);
        register("traitFullBright", FullBrightTrait.class);
        register("traitDarknessAura", DarknessAuraTrait.class);
        register("abilitySpecialFrogHop", com.iiampanda.morph.api.mob.trait.ability.SpecialFrogHopAbility.class);
        register("traitDolphinLeap", DolphinLeapTrait.class);
        register("traitDolphinsGrace", DolphinsGraceTrait.class);
        register("traitEmissive", EmissiveTrait.class);

        register("abilityClimb", ClimbAbility.class);
        register("abilityEffectAttack", EffectAttackAbility.class);
        register("abilityFlight", FlyAbility.class);
        register("abilityFlightFlap", FlightFlapAbility.class);
        register("abilityRideable", RideableAbility.class);
        register("abilitySlowFall", SlowFallAbility.class);
        register("abilityShearable", ShearableAbility.class);
        register("abilityCanBucket", CanBucketAbility.class);
        register("abilitySpecialSonicBoom", SpecialSonicBoomAbility.class);
        register("abilitySpecialProjectile", SpecialProjectileAbility.class);
        register("abilitySpecialShockwave", SpecialShockwaveAbility.class);
        register("abilitySpecialSelfDetonate", SpecialSelfDetonateAbility.class);
        register("abilitySpecialRollUp", SpecialRollUpAbility.class);
        register("abilitySpecialFangs", SpecialFangsAbility.class);
        register("abilitySpecialCommand", SpecialCommandAbility.class);
    }

    private TraitHandler() {}

    /** Other mods can register their own traits here. */
    public static void register(String type, Class<? extends Trait> clz)
    {
        if(TRAITS.containsKey(type))
        {
            LOGGER.warn("Trait type {} is already registered ({}). Overriding with {}", type, TRAITS.get(type).getName(), clz.getName());
        }
        TRAITS.put(type, clz);
    }

    public static Set<String> types()
    {
        return Collections.unmodifiableSet(TRAITS.keySet());
    }

    public static Trait<?> create(String type)
    {
        Class<? extends Trait> clz = TRAITS.get(type);
        if(clz == null)
        {
            return null;
        }
        try
        {
            return clz.getDeclaredConstructor().newInstance();
        }
        catch(Exception e)
        {
            LOGGER.error("Could not create trait {}", type, e);
            return null;
        }
    }

    public static Trait<?> copy(Trait<?> trait)
    {
        return GSON.fromJson(GSON.toJsonTree(trait, Trait.class), Trait.class);
    }

    public static String toJson(List<Trait<?>> traits)
    {
        JsonArray array = new JsonArray();
        for(Trait<?> trait : traits)
        {
            array.add(GSON.toJsonTree(trait, Trait.class));
        }
        return new Gson().toJson(array);
    }

    public static ArrayList<Trait<?>> fromJson(String json)
    {
        ArrayList<Trait<?>> traits = new ArrayList<>();
        try
        {
            for(JsonElement el : JsonParser.parseString(json).getAsJsonArray())
            {
                Trait<?> t = GSON.fromJson(el, Trait.class);
                if(t != null)
                {
                    traits.add(t);
                }
            }
        }
        catch(Exception e)
        {
            LOGGER.error("Could not parse trait list", e);
        }
        return traits;
    }

    private static class Adapter implements JsonDeserializer<Trait>, JsonSerializer<Trait>
    {
        private static final Gson PLAIN = new GsonBuilder().disableHtmlEscaping().create();

        @Override
        public Trait deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject obj = json.getAsJsonObject();
            if(!obj.has("type"))
            {
                throw new JsonParseException("Trait has no type");
            }
            String type = obj.get("type").getAsString();
            Class<? extends Trait> clz = TRAITS.get(type);
            if(clz == null)
            {
                LOGGER.error("Unknown trait type: {}", type);
                return null;
            }
            Trait trait = PLAIN.fromJson(obj, clz);
            trait.type = type;
            return trait;
        }

        @Override
        public JsonElement serialize(Trait src, Type typeOfSrc, JsonSerializationContext context)
        {
            return PLAIN.toJsonTree(src, src.getClass());
        }
    }
}
