package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.mob.VariantFactory;
import com.iiampanda.morph.common.morph.MorphManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

/** Kill a mob, acquire it. */
public class ClassicMode extends BaseMode
{
    @Override public String name() { return "classic"; }

    @Override
    public void onKill(ServerPlayer player, LivingEntity killed)
    {
        MorphVariant variant = VariantFactory.create(killed);
        if(variant != null && canMorphAtAll(player))
        {
            variant = MorphManager.restrictVariant(player, variant);
            if(canAcquire(player, variant) && MorphManager.acquire(player, variant))
            {
                MorphManager.sendAcquisition(player, killed, true, false);
            }
        }
    }

    protected boolean canMorphAtAll(Player player)
    {
        return allowed(player);
    }

    @Override public boolean canShowSelector(Player player) { return selectorAllowed(player); }

    @Override public boolean canMorph(Player player) { return allowed(player) && notMidMorph(player); }

    @Override public boolean canMorphEver(Player player) { return allowed(player); }

    @Override
    public boolean canAcquire(Player player, MorphVariant variant)
    {
        return allowed(player) && !MorphManager.data((ServerPlayer)player).contains(variant);
    }

    @Override
    public ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant)
    {
        return profileTraits(player, variant, Morph.configServer.classicUpgradeTraits);
    }
}
