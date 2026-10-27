package com.iiampanda.morph.client;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.TraitHooks;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.client.gui.BiomassScreen;
import com.iiampanda.morph.client.gui.MorphSelectorScreen;
import com.iiampanda.morph.common.packet.Packets;
import com.mojang.blaze3d.platform.InputConstants;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MorphClient
{
    private static final String CATEGORY = "key.categories.morph";

    public static KeyMapping KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_NEXT_FAV, KEY_PREV_FAV;

    /** All of the mod's keys, for the loader module to register. */
    public static List<KeyMapping> createKeys()
    {
        KEY_SELECTOR = new KeyMapping("key.morph.selector", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
        KEY_BIOMASS = new KeyMapping("key.morph.biomass", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
        KEY_ABILITIES = new KeyMapping("key.morph.abilities", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
        KEY_DEMORPH = new KeyMapping("key.morph.demorph", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
        KEY_NEXT_FAV = new KeyMapping("key.morph.nextFavourite", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);
        KEY_PREV_FAV = new KeyMapping("key.morph.prevFavourite", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
        return List.of(KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_NEXT_FAV, KEY_PREV_FAV);
    }

    /** Called by the loader module on the client, after the platform is set. */
    public static void init()
    {
        ClientPackets.register();
    }

    public static void onDisconnect()
    {
        ClientState.reset();
        TraitHooks.active().clear();
    }

    private static boolean prevUse;

    public static void tick(Minecraft mc)
    {
        MorphBodies.tick(mc);
        AcquisitionEffects.tick(mc);
        if(mc.player == null)
        {
            return;
        }
        boolean use = mc.options.keyUse.isDown();
        if(use && !prevUse && mc.screen == null && !mc.player.isSpectator() && hasSpecial(mc))
        {
            Platform.sendToServer(new Packets.UseSpecial());
        }
        prevUse = use;
        while(KEY_SELECTOR.consumeClick())
        {
            if(mc.screen == null)
            {
                if(ClientState.canSelect)
                {
                    mc.setScreen(new MorphSelectorScreen());
                }
                else
                {
                    mc.player.displayClientMessage(Component.translatable("morph.message.selectorDisabled"), true);
                }
            }
        }
        while(KEY_BIOMASS.consumeClick())
        {
            if(mc.screen == null)
            {
                if(ClientState.usesBiomass() && ClientState.biomassUnlocked)
                {
                    mc.setScreen(new BiomassScreen(BiomassScreen.Tab.UPGRADES));
                }
                else
                {
                    mc.player.displayClientMessage(Component.translatable(ClientState.usesBiomass() ? "morph.message.biomassLocked" : "morph.message.biomassNotInMode"), true);
                }
            }
        }
        while(KEY_ABILITIES.consumeClick())
        {
            if(mc.screen == null)
            {
                mc.setScreen(new BiomassScreen(BiomassScreen.Tab.ABILITIES));
            }
        }
        while(KEY_DEMORPH.consumeClick())
        {
            if(mc.screen == null && MorphApi.getMorphInfo(mc.player).isMorphed())
            {
                Platform.sendToServer(new Packets.RequestMorph(Optional.empty()));
            }
        }
        while(KEY_NEXT_FAV.consumeClick())
        {
            cycle(mc, 1);
        }
        while(KEY_PREV_FAV.consumeClick())
        {
            cycle(mc, -1);
        }
    }

    private static boolean hasSpecial(Minecraft mc)
    {
        for(var t : MorphApi.getMorphInfo(mc.player).traits)
        {
            if(t instanceof com.iiampanda.morph.api.mob.trait.ability.SpecialAbility<?>)
            {
                return true;
            }
        }
        return false;
    }

    /** Steps through [your own form, favourite 1, favourite 2, ...]. */
    private static void cycle(Minecraft mc, int dir)
    {
        if(mc.screen != null || !ClientState.canMorph)
        {
            return;
        }
        List<MorphVariant> favs = new ArrayList<>();
        for(MorphVariant v : ClientState.DATA.morphs)
        {
            if(ClientState.DATA.isFavourite(v))
            {
                favs.add(v);
            }
        }
        if(favs.isEmpty())
        {
            mc.player.displayClientMessage(Component.translatable("morph.message.noFavourites"), true);
            return;
        }
        MorphVariant cur = MorphApi.getMorphInfo(mc.player).variant;
        int idx = cur == null ? -1 : favs.indexOf(cur);
        int next = idx + dir;
        int count = favs.size() + 1; // +1 for the player's own form
        int slot = Math.floorMod(next + 1, count) - 1;
        if(slot < 0)
        {
            Platform.sendToServer(new Packets.RequestMorph(Optional.empty()));
        }
        else
        {
            Platform.sendToServer(new Packets.RequestMorph(Optional.of(favs.get(slot).write())));
        }
    }
}
