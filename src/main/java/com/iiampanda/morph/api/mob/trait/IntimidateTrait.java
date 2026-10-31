package com.iiampanda.morph.api.mob.trait;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;

import java.util.List;

public class IntimidateTrait extends Trait<IntimidateTrait>
{
    public List<String> entitiesToIntimidate;
    public Double distance;
    public Double farRunSpeed;
    public Double nearRunSpeed;

    public IntimidateTrait() { type = "traitIntimidate"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.level().isClientSide() || entitiesToIntimidate == null || player.tickCount % 5 != 0 || player.isSpectator())
        {
            return;
        }
        double dist = distance == null ? 6D : distance;
        double far = farRunSpeed == null ? 1D : farRunSpeed;
        double near = nearRunSpeed == null ? 1.2D : nearRunSpeed;
        for(PathfinderMob mob : player.level().getEntitiesOfClass(PathfinderMob.class, player.getBoundingBox().inflate(dist)))
        {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if(!entitiesToIntimidate.contains(id.toString()))
            {
                continue;
            }
            Vec3 away = DefaultRandomPos.getPosAway(mob, 16, 7, player.position());
            if(away != null && player.distanceToSqr(away) > player.distanceToSqr(mob))
            {
                mob.getNavigation().moveTo(away.x, away.y, away.z, mob.distanceTo(player) < dist / 2D ? near : far);
            }
        }
    }
}
