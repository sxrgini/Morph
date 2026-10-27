package com.iiampanda.morph.api.mob.trait.ability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;

/**
 * A "special move" for bosses and mini bosses, used by right clicking while morphed into the mob.
 * Mod makers / modpack creators can add any of the special ability types to a mob's JSON file and tune the values.
 */
public abstract class SpecialAbility<T extends SpecialAbility> extends Ability<T>
{
    /** Ticks before it can be used again. */
    public Integer cooldown;
    /** Optional sound id played when it is used. */
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

    /** @return true if the move was used */
    public final boolean tryActivate(ServerPlayer sp)
    {
        if(!enabled)
        {
            return false;
        }
        long now = sp.level().getGameTime();
        if(now < readyAt)
        {
            sp.sendOverlayMessage(Component.translatable("morph.special.cooldown", (readyAt - now + 19) / 20));
            return false;
        }
        if(!spendUse())
        {
            sp.sendOverlayMessage(Component.translatable("morph.special.noBiomass"));
            return false;
        }
        playConfiguredSound(sp);
        if(activate(sp))
        {
            readyAt = now + getCooldown();
            sp.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            return true;
        }
        return false;
    }

    protected abstract boolean activate(ServerPlayer player);

    /** "namespace:sound", "event:1016" (a vanilla level event, e.g. the ghast's warn / shoot sounds) or several of them separated by commas. */
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
                    sp.level().levelEvent(null, Integer.parseInt(part.substring(6).trim()), sp.blockPosition(), 0);
                }
                catch(NumberFormatException ignored) {}
                continue;
            }
            Identifier id = Identifier.tryParse(part);
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
