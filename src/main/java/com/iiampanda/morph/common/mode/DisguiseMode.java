package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.mob.VariantFactory;
import com.iiampanda.morph.common.morph.MorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public class DisguiseMode extends BaseMode
{
    @Override public String name() { return "disguise"; }

    @Override
    public void onKill(ServerPlayer player, LivingEntity killed)
    {
        MorphVariant variant = VariantFactory.create(killed);
        if(variant != null && canMorph(player))
        {
            variant = MorphManager.restrictVariant(player, variant);
            MorphManager.morph(player, variant, true);
            MorphManager.sendAcquisition(player, killed, true, false);
        }
    }

    @Override public boolean canShowSelector(Player player) { return false; }

    @Override public boolean canMorph(Player player) { return allowed(player) && notMidMorph(player); }

    @Override public boolean canMorphEver(Player player) { return allowed(player); }

    @Override public boolean canAcquire(Player player, MorphVariant variant) { return false; }

    @Override
    public ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant)
    {
        return profileTraits(player, variant, false);
    }
}
