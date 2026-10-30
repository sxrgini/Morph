package com.iiampanda.morph.api.mob.trait;

/** Hurt while standing in a biome where snow golems melt. */
public class SnowMeltTrait extends Trait<SnowMeltTrait>
{
    public SnowMeltTrait() { type = "traitSnowMelt"; }

    @Override
    public void tick(float strength)
    {
        if(strength < 1F || player.level().isClientSide || player.isCreative() || player.isSpectator() || player.tickCount % 10 != 0)
        {
            return;
        }
        if(player.level().getBiome(player.blockPosition()).is(net.minecraft.tags.BiomeTags.SNOW_GOLEM_MELTS))
        {
            player.hurt(player.damageSources().onFire(), 1F);
        }
    }
}
