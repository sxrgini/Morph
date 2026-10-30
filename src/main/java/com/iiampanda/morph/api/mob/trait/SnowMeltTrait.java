package com.iiampanda.morph.api.mob.trait;

/** Hurt while standing in a biome where snow golems melt. */
public class SnowMeltTrait extends Trait<SnowMeltTrait>
{
    public SnowMeltTrait() { type = "traitSnowMelt"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || !(player.level() instanceof net.minecraft.server.level.ServerLevel level) || player.isCreative() || player.isSpectator() || player.tickCount % 10 != 0)
        {
            return;
        }
        if(level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SNOW_GOLEM_MELTS, player.position()))
        {
            player.hurt(player.damageSources().onFire(), 1F);
        }
    }
}
