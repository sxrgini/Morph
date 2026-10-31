package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

public abstract class SpecialAbility<T extends SpecialAbility> extends Ability<T>
{

    public Integer cooldown;

    public String sound;

    public transient long readyAt;

    protected int defaultCooldown() { return 200; }

    @Override protected double defaultCost() { return 5D; }

    @Override protected String iconName() { return "hostile"; }

    @Override public void tick(float strength) {}

    public int getCooldown()
    {
        return cooldown == null ? defaultCooldown() : Math.max(0, cooldown);
    }

    public final boolean tryActivate(ServerPlayer sp)
    {
        if(!enabled)
        {
            return false;
        }
        long now = sp.level().getGameTime();
        if(now < readyAt)
        {
            sp.displayClientMessage(Component.translatable("morph.special.cooldown", (readyAt - now + 19) / 20), true);
            return false;
        }
        if(!spendUse())
        {
            sp.displayClientMessage(Component.translatable("morph.special.noBiomass"), true);
            return false;
        }
        playConfiguredSound(sp);
        if(activate(sp))
        {
            readyAt = now + getCooldown();
            sp.swing(InteractionHand.MAIN_HAND, true);
            return true;
        }
        return false;
    }

    protected abstract boolean activate(ServerPlayer player);

    protected void playConfiguredSound(ServerPlayer sp)
    {
        if(sound == null || sound.isEmpty())
        {
            return;
        }
        for(String part : sound.split(","))
        {
            part = part.trim();
            if(part.startsWith("event:"))
            {
                try
                {
                    sp.serverLevel().levelEvent(null, Integer.parseInt(part.substring(6).trim()), sp.blockPosition(), 0);
                }
                catch(NumberFormatException ignored) {}
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(part);
            if(id != null)
            {
                BuiltInRegistries.SOUND_EVENT.getOptional(id).ifPresent(ev -> play(sp, ev, 1F));
            }
        }
    }

    protected static void play(ServerPlayer sp, SoundEvent ev, float pitch)
    {
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), ev, SoundSource.PLAYERS, 2F, pitch);
    }
}
