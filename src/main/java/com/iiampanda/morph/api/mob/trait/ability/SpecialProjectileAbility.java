package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.phys.Vec3;

/** Shoots any entity as a projectile: dragon fireballs, wither skulls, fireballs, or anything a mod adds. */
public class SpecialProjectileAbility extends SpecialAbility<SpecialProjectileAbility>
{
    /** Entity type id to shoot. */
    public String entity;
    public Double speed;
    public Integer count;
    /** Spread: degrees between projectiles when count > 1, and random inaccuracy. */
    public Double spreadDegrees;
    public Double inaccuracy;
    /** Optional SNBT merged into the projectile, e.g. {ExplosionPower:3}. */
    public String nbt;
    /** Wither skulls only: the blue, block-breaking kind. */
    public Boolean dangerous;

    public SpecialProjectileAbility() { type = "abilitySpecialProjectile"; }

    @Override protected int defaultCooldown() { return 60; }

    @Override
    protected boolean activate(ServerPlayer p)
    {
        ResourceLocation id = ResourceLocation.tryParse(entity == null ? "minecraft:fireball" : entity);
        EntityType<?> et = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if(et == null)
        {
            return false;
        }
        ServerLevel level = p.serverLevel();
        int n = Math.max(1, count == null ? 1 : count);
        double step = Math.toRadians(spreadDegrees == null ? 10D : spreadDegrees);
        Vec3 look = p.getLookAngle();
        boolean spawned = false;
        for(int i = 0; i < n; i++)
        {
            Vec3 dir = look.yRot((float)((i - (n - 1) / 2D) * step));
            Entity e = et.create(level);
            if(e == null)
            {
                continue;
            }
            if(nbt != null && !nbt.isEmpty())
            {
                try
                {
                    CompoundTag tag = e.saveWithoutId(new CompoundTag());
                    tag.merge(TagParser.parseTag(nbt));
                    e.load(tag);
                }
                catch(Exception ex)
                {
                    // bad SNBT in the config: ignore it and shoot the plain projectile
                }
            }
            Vec3 pos = p.getEyePosition().add(dir.scale(1.2D)).add(0D, -0.2D, 0D);
            e.moveTo(pos.x, pos.y, pos.z, p.getYRot(), p.getXRot());
            float sp = (float)(speed == null ? 1.5D : speed);
            if(e instanceof Projectile pr)
            {
                pr.setOwner(p);
                pr.shoot(dir.x, dir.y, dir.z, sp, (float)(inaccuracy == null ? 0D : inaccuracy));
            }
            else
            {
                e.setDeltaMovement(dir.scale(sp));
            }
            if(e instanceof WitherSkull skull && dangerous != null)
            {
                skull.setDangerous(dangerous);
            }
            level.addFreshEntity(e);
            spawned = true;
        }
        return spawned;
    }

    @Override
    public String getTranslationKeyRoot()
    {
        if("minecraft:dragon_fireball".equals(entity))
        {
            return super.getTranslationKeyRoot() + ".dragon";
        }
        if("minecraft:wither_skull".equals(entity))
        {
            return super.getTranslationKeyRoot() + ".wither";
        }
        if("minecraft:fireball".equals(entity))
        {
            return super.getTranslationKeyRoot() + ".ghast";
        }
        return super.getTranslationKeyRoot();
    }
}
