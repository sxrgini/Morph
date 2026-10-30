package com.iiampanda.morph.common.mob;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.*;
import com.iiampanda.morph.common.Morph;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.animal.golem.IronGolem;

import java.lang.reflect.Field;

/** Boss detection, default special moves, and the right-click handler. */
public final class SpecialAbilities
{
    private SpecialAbilities() {}

    /** Special move for the vanilla bosses / mini bosses; null for everything else. */
    public static SpecialAbility<?> defaultForVanilla(Identifier id)
    {
        if(!"minecraft".equals(id.getNamespace()))
        {
            return null;
        }
        switch(id.getPath())
        {
            case "warden" ->
            {
                SpecialSonicBoomAbility s = new SpecialSonicBoomAbility();
                s.damage = 10D;
                s.range = 20D;
                s.cooldown = 160;
                return s;
            }
            case "ender_dragon" ->
            {
                SpecialProjectileAbility s = new SpecialProjectileAbility();
                s.entity = "minecraft:dragon_fireball";
                s.speed = 1.2D;
                s.cooldown = 80;
                s.sound = "minecraft:entity.ender_dragon.shoot";
                return s;
            }
            case "wither" ->
            {
                SpecialProjectileAbility s = new SpecialProjectileAbility();
                s.entity = "minecraft:wither_skull";
                s.dangerous = true;
                s.speed = 1.6D;
                s.cooldown = 40;
                s.sound = "minecraft:entity.wither.shoot";
                return s;
            }
            case "elder_guardian" ->
            {
                SpecialShockwaveAbility s = new SpecialShockwaveAbility();
                s.radius = 14D;
                s.knockback = 0D;
                s.effect = "minecraft:mining_fatigue";
                s.effectDuration = 1200;
                s.effectAmplifier = 2;
                s.onlyPlayers = true;
                s.cooldown = 600;
                s.sound = "minecraft:entity.elder_guardian.curse";
                return s;
            }
            case "ravager" ->
            {
                SpecialShockwaveAbility s = new SpecialShockwaveAbility();
                s.radius = 6D;
                s.damage = 6D;
                s.knockback = 1.8D;
                s.cooldown = 120;
                s.sound = "minecraft:entity.ravager.roar";
                return s;
            }
            case "creeper" ->
            {
                SpecialSelfDetonateAbility s = new SpecialSelfDetonateAbility();
                s.cooldown = 61;
                return s;
            }
            case "ghast" ->
            {
                SpecialProjectileAbility s = new SpecialProjectileAbility();
                s.entity = "minecraft:fireball";
                s.speed = 1.2D;
                s.nbt = "{ExplosionPower:1}";
                s.cooldown = 60;
                s.sound = "event:1015,event:1016";
                return s;
            }
            case "blaze" ->
            {
                SpecialProjectileAbility s = new SpecialProjectileAbility();
                s.entity = "minecraft:small_fireball";
                s.speed = 1.2D;
                s.count = 3;
                s.spreadDegrees = 6D;
                s.cooldown = 30;
                s.sound = "minecraft:entity.blaze.shoot";
                return s;
            }
            case "armadillo" ->
            {
                SpecialRollUpAbility s = new SpecialRollUpAbility();
                s.duration = 100;
                s.damageReduction = 0.5D;
                s.cooldown = 300;
                return s;
            }
            case "evoker" ->
            {
                SpecialFangsAbility s = new SpecialFangsAbility();
                s.cooldown = 100;
                return s;
            }
            default -> { return null; }
        }
    }

    /** Heuristic for modded bosses: boss tag, a boss bar field, very high health, or a hostile with high health. */
    public static boolean isBoss(LivingEntity e)
    {
        if(e instanceof net.minecraft.world.entity.player.Player)
        {
            return false;
        }
        EntityType<?> type = e.getType();
        if(type.builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "bosses")))
                || type.builtInRegistryHolder().is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("c", "mini_bosses"))))
        {
            return true;
        }
        for(Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for(Field f : c.getDeclaredFields())
            {
                if(net.minecraft.world.BossEvent.class.isAssignableFrom(f.getType()))
                {
                    return true;
                }
            }
        }
        double health = e.getAttributeBaseValue(Attributes.MAX_HEALTH);
        return !(e instanceof IronGolem) && (health >= 100D || (e instanceof Enemy && health >= 80D));
    }

    /** The move a mob gets by default: the vanilla one, or a random (but stable per mob type) pick for other bosses. */
    public static SpecialAbility<?> defaultFor(LivingEntity e)
    {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        SpecialAbility<?> vanilla = defaultForVanilla(id);
        if(vanilla != null)
        {
            return vanilla;
        }
        if("minecraft".equals(id.getNamespace()) || !isBoss(e))
        {
            return null;
        }
        switch(Math.floorMod(id.toString().hashCode(), 4))
        {
            case 0 ->
            {
                SpecialShockwaveAbility s = new SpecialShockwaveAbility();
                s.radius = 8D;
                s.damage = 8D;
                s.knockback = 2D;
                s.cooldown = 140;
                s.sound = "minecraft:entity.ravager.roar";
                return s;
            }
            case 1 ->
            {
                SpecialProjectileAbility s = new SpecialProjectileAbility();
                s.entity = "minecraft:fireball";
                s.speed = 1.2D;
                s.nbt = "{ExplosionPower:2}";
                s.cooldown = 80;
                s.sound = "minecraft:entity.blaze.shoot";
                return s;
            }
            case 2 ->
            {
                SpecialSonicBoomAbility s = new SpecialSonicBoomAbility();
                s.damage = 8D;
                s.range = 16D;
                s.cooldown = 160;
                return s;
            }
            default ->
            {
                SpecialFangsAbility s = new SpecialFangsAbility();
                s.count = 10;
                s.cooldown = 120;
                return s;
            }
        }
    }

    public static boolean hasSpecial(MobProfile profile)
    {
        if(profile.traits != null)
        {
            for(Trait<?> t : profile.traits)
            {
                if(t instanceof SpecialAbility<?>)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** Adds the default move to a profile that has none and has not been looked at yet. @return true if the file needs saving. */
    public static boolean upgrade(MobProfile profile, SpecialAbility<?> def)
    {
        if(profile.specialsHandled != null)
        {
            return false;
        }
        profile.specialsHandled = true;
        if(def != null && !hasSpecial(profile))
        {
            profile.traits.add(def);
        }
        return def != null;
    }

    /** Right click while morphed (sent by the client). */
    public static void use(ServerPlayer player)
    {
        if(Morph.configServer != null && !Morph.configServer.specialAbilities)
        {
            return;
        }
        if(Morph.configServer == null || Morph.configServer.specialRequiresEmptyHand)
        {
            if(!player.getMainHandItem().isEmpty())
            {
                return;
            }
        }
        for(Trait<?> t : MorphApi.getMorphInfo(player).traits)
        {
            if(t instanceof SpecialAbility<?> s && s.tryActivate(player))
            {
                return;
            }
        }
    }
}
