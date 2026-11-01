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
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("morph", "morph"));

    public static KeyMapping KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_SIT, KEY_LAST;

    public static List<KeyMapping> createKeys()
    {
        KEY_SELECTOR = new KeyMapping("key.morph.selector", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
        KEY_BIOMASS = new KeyMapping("key.morph.biomass", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
        KEY_ABILITIES = new KeyMapping("key.morph.abilities", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
        KEY_DEMORPH = new KeyMapping("key.morph.demorph", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
        KEY_SIT = new KeyMapping("key.morph.sit", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
        KEY_LAST = new KeyMapping("key.morph.last", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY);
        return List.of(KEY_SELECTOR, KEY_BIOMASS, KEY_ABILITIES, KEY_DEMORPH, KEY_SIT, KEY_LAST);
    }

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
        com.iiampanda.morph.common.MorphRules.clientInventory = true;
        com.iiampanda.morph.common.MorphRules.clientInteract = true;
        com.iiampanda.morph.common.MorphRules.clientCrafting = true;
        com.iiampanda.morph.common.MorphRules.clientHotbarOnly = false;
        com.iiampanda.morph.common.MorphRules.clientSlots = 9;
        com.iiampanda.morph.common.MorphRules.clientNormalTicks = 0;
        com.iiampanda.morph.common.MorphRules.clientCooldownTicks = 0;
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
        if((mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen || mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) && !com.iiampanda.morph.common.MorphRules.inventoryAllowed(mc.player))
        {
            mc.gui.setScreen(null);
        }
        if(!emissiveReported && mc.level != null)
        {
            emissiveReported = true;
            EmissiveScanner.scanAndReport(mc);
        }
        com.iiampanda.morph.common.MorphRules.clampClient(mc.player);
        com.iiampanda.morph.common.MorphRules.clientTimers();
        boolean use = mc.options.keyUse.isDown();
        if(use && !prevUse && mc.gui.screen() == null && !mc.player.isSpectator() && hasSpecial(mc))
        {
            Platform.sendToServer(new Packets.UseSpecial());
        }
        prevUse = use;
        if(ClientState.oneMorph())
        {
            while(KEY_SELECTOR.consumeClick() | KEY_BIOMASS.consumeClick() | KEY_ABILITIES.consumeClick())
            {
                mc.player.sendOverlayMessage(Component.translatable("morph.message.guiDisabledOneMorph"));
            }
        }
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
        while(KEY_LAST.consumeClick())
        {
            if(mc.gui.screen() == null)
            {
                MorphVariant last = ClientState.DATA.last;
                MorphVariant now = MorphApi.getMorphInfo(mc.player).variant;
                if(last == null)
                {
                    mc.player.sendOverlayMessage(Component.translatable("morph.message.noLastMorph"));
                }
                else if(now == null || !now.equals(last))
                {
                    Platform.sendToServer(new Packets.RequestMorph(Optional.of(last.write())));
                }
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
