package com.iiampanda.morph.common.mode;

import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.morph.MorphManager;
import com.iiampanda.morph.common.morph.OneMorph;
import com.iiampanda.morph.common.save.PlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public class OneMorphMode extends BaseMode
{
    @Override public String name() { return "one_morph"; }

    @Override public void onKill(ServerPlayer player, LivingEntity killed) {}

    @Override public boolean canShowSelector(Player player) { return false; }

    @Override public boolean canMorph(Player player) { return allowed(player) && notMidMorph(player); }

    @Override public boolean canMorphEver(Player player) { return allowed(player); }

    @Override public boolean canAcquire(Player player, MorphVariant variant) { return false; }

    @Override
    public boolean owns(ServerPlayer player, MorphVariant variant)
    {
        MorphVariant mine = MorphManager.data(player).oneMorph;
        return mine != null && mine.equals(variant);
    }

    @Override
    public void onJoin(ServerPlayer player)
    {
        if(MorphManager.data(player).oneMorph == null)
        {
            OneMorph.assign(player);
        }
    }

    @Override
    public boolean canDemorph(ServerPlayer player)
    {
        PlayerData data = MorphManager.data(player);
        long now = OneMorph.now(player);
        if(data.oneMorph != null && data.oneCooldownUntil > now)
        {
            player.displayClientMessage(Component.translatable("morph.message.oneMorphCooldown", OneMorph.format(data.oneCooldownUntil - now)), true);
            return false;
        }
        return true;
    }

    @Override
    public void onMorphed(ServerPlayer player, MorphVariant variant)
    {
        PlayerData data = MorphManager.data(player);
        long now = OneMorph.now(player);
        if(variant == null)
        {
            data.oneNormalUntil = now + OneMorph.NORMAL_TICKS;
        }
        else if(data.oneMorph != null && variant.equals(data.oneMorph))
        {
            data.oneNormalUntil = 0L;
            data.oneCooldownUntil = now + OneMorph.COOLDOWN_TICKS;
        }
        com.iiampanda.morph.common.MorphRules.forget(player);
        com.iiampanda.morph.common.MorphRules.sync(player);
    }

    @Override
    public void onRespawn(ServerPlayer player, boolean alive)
    {
        PlayerData data = MorphManager.data(player);
        if(!alive && data.oneMorph != null)
        {
            data.current = data.oneMorph;
            data.oneNormalUntil = 0L;
            OneMorph.returnToHabitat(player);
        }
    }

    @Override
    public ArrayList<Trait<?>> traitsFor(Player player, MorphVariant variant)
    {
        return profileTraits(player, variant, Morph.configServer.classicUpgradeTraits);
    }
}
