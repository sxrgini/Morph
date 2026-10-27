package com.iiampanda.morph.client;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.Ability;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.common.biomass.Upgrades;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class HudOverlay
{
    private HudOverlay() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || mc.getDebugOverlay().showDebugScreen())
        {
            return;
        }
        int h = g.guiHeight();
        int w = g.guiWidth();

        if(ClientState.usesBiomass() && ClientState.biomassUnlocked)
        {
            double max = Upgrades.maxBiomass(ClientState.DATA);
            double cur = ClientState.DATA.biomass;
            int x = 6, y = 6, bw = 90;
            g.fill(x - 1, y - 1, x + bw + 1, y + 8, 0xFF000000);
            g.fill(x, y, x + bw, y + 7, 0xFF2A1A1A);
            g.fill(x, y, x + (int)(bw * Math.min(1D, cur / max)), y + 7, 0xFFB03A2E);
            String text = String.format(Locale.ROOT, "%.0f / %.0f", cur, max);
            g.text(mc.font, Component.translatable("morph.hud.biomass"), x, y + 10, 0xFFFFCCCC, true);
            g.text(mc.font, text, x + bw - mc.font.width(text), y + 10, 0xFFFFFFFF, true);
        }

        if(FrogHopClient.charge > 0F)
        {
            int bx = w / 2 - 91, by = h - 29;
            g.fill(bx - 1, by - 1, bx + 183, by + 6, 0xFF000000);
            g.fill(bx, by, bx + 182, by + 5, 0xFF333333);
            g.fill(bx, by, bx + (int)(182 * FrogHopClient.charge), by + 5, 0xFF55DD55);
        }

        MorphInfo info = MorphApi.getMorphInfo(mc.player);
        if(info.isMorphed() && info.variant != null)
        {
            int yy = ClientState.usesBiomass() && ClientState.biomassUnlocked ? 28 : 6;
            g.text(mc.font, info.variant.displayName(), 6, yy, 0xFFFFFFFF, true);

            int x = w - 22;
            int y = h - 24;
            for(Trait<?> t : info.traits)
            {
                if(!(t instanceof Ability<?> a) || t.isHidden())
                {
                    continue;
                }
                g.fill(x - 1, y - 1, x + 17, y + 17, a.enabled ? 0xFF2E8B2E : 0xFF553333);
                Identifier icon = a.texIcon();
                if(icon != null)
                {
                    boolean eff18 = icon.getNamespace().equals("minecraft"); // sz18
                    int sz = eff18 ? 18 : 16;
                    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, eff18 ? x - 1 : x, eff18 ? y - 1 : y, 0F, 0F, sz, sz, sz, sz);
                }
                x -= 20;
            }
        }
    }
}
