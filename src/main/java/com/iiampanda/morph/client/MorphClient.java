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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MorphClient
{
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("morph", "morph"));

    public static KeyMapping KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_SIT;

    /** All of the mod's keys, for the loader module to register. */
    public static List<KeyMapping> createKeys()
    {
        KEY_SELECTOR = new KeyMapping("key.morph.selector", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY);
        KEY_BIOMASS = new KeyMapping("key.morph.biomass", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY);
        KEY_ABILITIES = new KeyMapping("key.morph.abilities", InputConstants.Type.KEYBOARD, InputConstants.KEY_V, CATEGORY);
        KEY_DEMORPH = new KeyMapping("key.morph.demorph", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, CATEGORY);
        KEY_SIT = new KeyMapping("key.morph.sit", InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, CATEGORY);
        return List.of(KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_SIT);
    }

    /** Called by the loader module on the client, after the platform is set. */
    public static void init()
    {
        ClientPackets.register();
    }

    private static boolean emissiveReported;

    public static void onDisconnect()
    {
        cameraBeforeHold = null;
        com.iiampanda.morph.common.morph.BucketHold.VIEW.clear();
        ClientState.SHEARED.clear();
        com.iiampanda.morph.api.mob.trait.SwallowBlocksTrait.SYNCED.clear();
        emissiveReported = false;
        com.iiampanda.morph.client.render.HandInfos.reset();
        ClientState.reset();
        TraitHooks.active().clear();
    }

    private static boolean prevUse;
    private static net.minecraft.client.CameraType cameraBeforeHold;

    public static void tick(Minecraft mc)
    {
        MorphBodies.tick(mc);
        AcquisitionEffects.tick(mc);
        if(mc.player == null)
        {
            return;
        }
        if(!emissiveReported && mc.level != null)
        {
            emissiveReported = true;
            EmissiveScanner.scanAndReport(mc);
        }
        boolean use = mc.options.keyUse.isDown();
        if(use && !prevUse && mc.gui.screen() == null && !mc.player.isSpectator() && hasSpecial(mc))
        {
            Platform.sendToServer(new Packets.UseSpecial());
        }
        prevUse = use;
        while(KEY_SELECTOR.consumeClick())
        {
            if(mc.gui.screen() == null)
            {
                if(ClientState.canSelect)
                {
                    mc.gui.setScreen(new MorphSelectorScreen());
                }
                else
                {
                    mc.player.sendOverlayMessage(Component.translatable("morph.message.selectorDisabled"));
                }
            }
        }
        while(KEY_BIOMASS.consumeClick())
        {
            if(mc.gui.screen() == null)
            {
                if(ClientState.usesBiomass() && ClientState.biomassUnlocked)
                {
                    mc.gui.setScreen(new BiomassScreen(BiomassScreen.Tab.UPGRADES));
                }
                else
                {
                    mc.player.sendOverlayMessage(Component.translatable(ClientState.usesBiomass() ? "morph.message.biomassLocked" : "morph.message.biomassNotInMode"));
                }
            }
        }
        while(KEY_ABILITIES.consumeClick())
        {
            if(mc.gui.screen() == null)
            {
                mc.gui.setScreen(new BiomassScreen(BiomassScreen.Tab.ABILITIES));
            }
        }
        while(KEY_DEMORPH.consumeClick())
        {
            if(mc.gui.screen() == null && MorphApi.getMorphInfo(mc.player).isMorphed())
            {
                Platform.sendToServer(new Packets.RequestMorph(Optional.empty()));
            }
        }
        while(KEY_SIT.consumeClick())
        {
            if(mc.gui.screen() == null && MorphApi.getMorphInfo(mc.player).isMorphed())
            {
                net.minecraft.world.entity.LivingEntity stand = MorphApi.getMorphInfo(mc.player).getEntity();
                boolean sitting = ClientState.SITTING.contains(mc.player.getUUID());
                com.iiampanda.morph.common.Dbg.log("sit key: stand-in={} canSit={} sitting={}", stand == null ? "none" : stand.getClass().getSimpleName(), stand != null && SitSupport.canSit(stand), sitting);
                if(stand != null && (sitting || SitSupport.canSit(stand)))
                {
                    Platform.sendToServer(new Packets.SetSit(!sitting));
                }
                else
                {
                    mc.player.sendOverlayMessage(Component.translatable("morph.message.cannotSit"));
                }
            }
        }
        net.minecraft.world.entity.Entity viewing = mc.getCameraEntity();
        boolean held = viewing != null && viewing != mc.player && !mc.player.isSpectator();
        if(held && cameraBeforeHold == null)
        {
            cameraBeforeHold = mc.options.getCameraType();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        }
        else if(!held && cameraBeforeHold != null)
        {
            mc.options.setCameraType(cameraBeforeHold);
            cameraBeforeHold = null;
        }
        // a sitting morph stands up when its player moves, jumps or sneaks
        if(ClientState.SITTING.contains(mc.player.getUUID()) && (mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0016D || mc.options.keyJump.isDown() || mc.player.isShiftKeyDown() || !mc.player.onGround()))
        {
            Platform.sendToServer(new Packets.SetSit(false));
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
}
