package com.iiampanda.morph.api.mob.trait;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;

/** Sprint up to the surface and you leap out of the water, like a dolphin. */
public class DolphinLeapTrait extends Trait<DolphinLeapTrait>
{
    public Double upward;     // vertical launch speed, default 0.9
    public Double forward;    // speed added in the look direction, default 0.6
    public Integer cooldown;  // ticks before the next leap, default 20

    private transient boolean wasUnder;
    private transient long lastLeap;

    public DolphinLeapTrait() { type = "traitDolphinLeap"; }

    @Override
    public void tick(float strength)
    {
        boolean under = player.isEyeInFluid(FluidTags.WATER);
        boolean was = wasUnder;
        wasUnder = under;
        if(strength < 1F || player.level().isClientSide() || player.isSpectator() || player.isPassenger())
        {
            return;
        }
        // head just broke the surface while sprinting upwards
        if(was && !under && player.isInWater() && player.isSprinting() && player.getDeltaMovement().y > 0.02D)
        {
            long now = player.level().getGameTime();
            if(now - lastLeap < (cooldown == null ? 20 : cooldown))
            {
                return;
            }
            lastLeap = now;
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0D, look.z);
            flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(forward == null ? 0.6D : forward);
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x + flat.x, Math.max(v.y, 0D) + (upward == null ? 0.9D : upward), v.z + flat.z);
            player.needsSync = true;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DOLPHIN_JUMP, SoundSource.PLAYERS, 1F, 1F);
        }
    }
}
