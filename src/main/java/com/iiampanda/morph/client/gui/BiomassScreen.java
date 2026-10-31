package com.iiampanda.morph.client.gui;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.ability.Ability;
import com.iiampanda.morph.api.morph.MorphInfo;
import com.iiampanda.morph.client.ClientState;
import com.iiampanda.morph.common.biomass.BiomassUpgradeInfo;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.*;

public class BiomassScreen extends Screen
{
    public enum Tab { UPGRADES, ABILITIES, MORPHS }

    private static final int NODE_W = 112, NODE_H = 38, COL_GAP = 28, ROW_GAP = 10;

    private Tab tab;
    private MorphGrid grid;
    private final List<Button> tabButtons = new ArrayList<>();
    private final Map<String, int[]> nodePos = new LinkedHashMap<>();
    private double panX, panY;
    private final List<AbilityRow> abilityRows = new ArrayList<>();

    private record AbilityRow(Trait<?> trait, int y) {}

    public BiomassScreen(Tab tab)
    {
        super(Component.translatable("morph.gui.biomass.title"));
        this.tab = tab;
    }

    @Override
    protected void init()
    {
        tabButtons.clear();
        int bw = 90;
        int x = width / 2 - (bw * 3 + 8) / 2;
        for(Tab t : Tab.values())
        {
            Button b = Button.builder(Component.translatable("morph.gui.tab." + t.name().toLowerCase(Locale.ROOT)), btn -> setTab(t)).bounds(x, 30, bw, 20).build();
            tabButtons.add(b);
            addRenderableWidget(b);
            x += bw + 4;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 50, height - 26, 100, 20).build());

        int gw = Math.min(width - 40, MorphGrid.TILE_W * 9);
        int gh = Math.min(height - 120, MorphGrid.TILE_H * 4);
        grid = new MorphGrid((width - gw) / 2, 58, gw, gh);
        grid.enabled = ClientState.canMorph;
        grid.onSelect = v -> Platform.sendToServer(new Packets.RequestMorph(Optional.of(v.write())));
        grid.onFavourite = v -> Platform.sendToServer(new Packets.SetFavourite(v.write(), !ClientState.DATA.isFavourite(v)));
        grid.onForget = v -> Platform.sendToServer(new Packets.ForgetMorph(v.write()));
        layoutTree();
        refreshTabs();
    }

    private void setTab(Tab t)
    {
        tab = t;
        refreshTabs();
    }

    private void refreshTabs()
    {
        for(int i = 0; i < tabButtons.size(); i++)
        {
            tabButtons.get(i).active = Tab.values()[i] != tab;
        }
    }

    public void dataChanged()
    {
        layoutTree();
    }

    private void layoutTree()
    {
        nodePos.clear();
        int[] row = {0};
        for(BiomassUpgradeInfo root : Upgrades.children(null))
        {
            place(root, 0, row);
        }
    }

    private double place(BiomassUpgradeInfo info, int depth, int[] nextRow)
    {
        List<BiomassUpgradeInfo> kids = Upgrades.children(info.id);
        double r;
        if(kids.isEmpty())
        {
            r = nextRow[0]++;
        }
        else
        {
            double sum = 0;
            for(BiomassUpgradeInfo k : kids)
            {
                sum += place(k, depth + 1, nextRow);
            }
            r = sum / kids.size();
        }
        nodePos.put(info.id, new int[]{depth * (NODE_W + COL_GAP), (int)Math.round(r * (NODE_H + ROW_GAP))});
        return r;
    }

    private int originX()
    {
        return 40 + (int)panX;
    }

    private int originY()
    {
        return 66 + (int)panY;
    }

    private void renderTree(GuiGraphicsExtractor g, int mx, int my)
    {
        g.enableScissor(10, 58, width - 10, height - 34);

        for(BiomassUpgradeInfo info : Upgrades.TREE.values())
        {
            int[] p = nodePos.get(info.id);
            int[] parent = info.parentId == null ? null : nodePos.get(info.parentId);
            if(p != null && parent != null)
            {
                int x1 = originX() + parent[0] + NODE_W, y1 = originY() + parent[1] + NODE_H / 2;
                int x2 = originX() + p[0], y2 = originY() + p[1] + NODE_H / 2;
                int midX = x1 + COL_GAP / 2;
                int color = Upgrades.level(ClientState.DATA, info.parentId) > 0 ? 0xFF7FD17F : 0xFF555555;
                g.horizontalLine(x1, midX, y1, color);
                g.verticalLine(midX, Math.min(y1, y2), Math.max(y1, y2), color);
                g.horizontalLine(midX, x2, y2, color);
            }
        }
        List<Component> tooltip = null;
        for(BiomassUpgradeInfo info : Upgrades.TREE.values())
        {
            int[] p = nodePos.get(info.id);
            if(p == null)
            {
                continue;
            }
            int nx = originX() + p[0], ny = originY() + p[1];
            int level = Upgrades.level(ClientState.DATA, info.id);
            boolean unlocked = Upgrades.parentUnlocked(ClientState.DATA, info.id);
            boolean maxed = Upgrades.isMaxed(ClientState.DATA, info.id);
            double cost = Upgrades.nextCost(ClientState.DATA, info.id);
            boolean affordable = Upgrades.canPurchase(ClientState.DATA, info.id);
            int border = !unlocked ? 0xFF444444 : maxed ? 0xFFE0C040 : affordable ? 0xFF55FF55 : level > 0 ? 0xFF88AAFF : 0xFFAA5555;
            boolean hover = mx >= nx && mx < nx + NODE_W && my >= ny && my < ny + NODE_H && my > 58 && my < height - 34;
            g.fill(nx, ny, nx + NODE_W, ny + NODE_H, hover ? 0xFF2A2A40 : 0xFF181826);
            g.outline(nx, ny, NODE_W, NODE_H, border);
            g.text(font, Component.translatable("morph.upgrade." + info.id + ".name"), nx + 5, ny + 5, unlocked ? 0xFFFFFFFF : 0xFF777777, false);
            String lv = info.maxLevel != null && info.maxLevel == 1 ? (level > 0 ? "✔" : "") : "Lv " + level + (info.maxLevel != null ? "/" + info.maxLevel : "");
            g.text(font, lv, nx + 5, ny + 17, 0xFFAAAAAA, false);
            Component costText = maxed ? Component.translatable("morph.gui.max") : Component.literal((long)cost + " BM");
            g.text(font, costText, nx + NODE_W - 5 - font.width(costText), ny + 27, affordable ? 0xFF55FF55 : 0xFFAA7777, false);
            if(hover)
            {
                tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("morph.upgrade." + info.id + ".name").withStyle(ChatFormatting.WHITE));
                tooltip.add(Component.translatable("morph.upgrade." + info.id + ".desc").withStyle(ChatFormatting.GRAY));
                if(info.value != null)
                {
                    tooltip.add(Component.translatable("morph.gui.upgrade.current", fmt(Upgrades.value(ClientState.DATA, info.id))).withStyle(ChatFormatting.AQUA));
                    if(!maxed)
                    {
                        tooltip.add(Component.translatable("morph.gui.upgrade.next", fmt(info.value.get(level + 1))).withStyle(ChatFormatting.GREEN));
                    }
                }
                if(!unlocked)
                {
                    tooltip.add(Component.translatable("morph.gui.upgrade.requires", Component.translatable("morph.upgrade." + info.parentId + ".name")).withStyle(ChatFormatting.RED));
                }
            }
        }
        g.disableScissor();
        if(tooltip != null)
        {
            g.setComponentTooltipForNextFrame(font, tooltip, mx, my);
        }
    }

    private static String fmt(double d)
    {
        return d == Math.rint(d) ? Long.toString((long)d) : String.format(Locale.ROOT, "%.2f", d);
    }

    private void renderAbilities(GuiGraphicsExtractor g, int mx, int my)
    {
        abilityRows.clear();
        MorphInfo info = MorphApi.getMorphInfo(minecraft.player);
        int x = Math.max(20, width / 2 - 170);
        int y = 62;
        if(!info.isMorphed())
        {
            g.centeredText(font, Component.translatable("morph.gui.abilities.notMorphed"), width / 2, y + 20, 0xFFAAAAAA);
            return;
        }
        g.text(font, Component.translatable("morph.gui.abilities.for", info.variant.displayName()), x, y - 2, 0xFFFFFFFF);
        y += 12;
        if(ClientState.usesBiomass() && !ClientState.abilitiesUnlocked)
        {
            g.text(font, Component.translatable("morph.gui.abilities.locked", Component.translatable("morph.upgrade." + Upgrades.ID_BIOMASS_ABILITIES + ".name")), x, y, 0xFFFFAA55);
            y += 14;
        }
        List<Trait<?>> sorted = new ArrayList<>(info.traits);
        sorted.sort(Comparator.comparing((Trait<?> t) -> !t.isAbility()).thenComparing(t -> t.type));
        if(sorted.isEmpty())
        {
            g.text(font, Component.translatable("morph.gui.abilities.none"), x, y + 6, 0xFFAAAAAA);
            return;
        }
        for(Trait<?> t : sorted)
        {
            if(t.isHidden() || y > height - 60)
            {
                continue;
            }
            boolean ability = t instanceof Ability<?>;
            boolean on = !(t instanceof Ability<?> a) || a.enabled;
            g.fill(x, y, x + 340, y + 26, 0xCC181826);
            Identifier icon = t.texIcon();
            int tx = icon == null ? x + 6 : x + 26;
            if(icon != null)
            {
                boolean eff = icon.getNamespace().equals("minecraft");
                int sz = eff ? 18 : 16;
                g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, icon, x + (eff ? 3 : 4), y + (eff ? 4 : 5), 0F, 0F, sz, sz, sz, sz);
            }
            g.text(font, Component.translatable(t.getTranslationKeyRoot() + ".name"), tx, y + 3, on ? 0xFFFFFFFF : 0xFF888888, false);
            Component desc = Component.translatable(t.getTranslationKeyRoot() + ".desc");
            g.text(font, font.plainSubstrByWidth(desc.getString(), 230 + (26 - (tx - x))), tx, y + 14, 0xFF999999, false);
            if(ability)
            {
                Ability<?> a = (Ability<?>)t;
                if(ClientState.usesBiomass() && a.getCost() > 0)
                {
                    Component c = Component.translatable("morph.gui.abilities.cost", fmt(a.getCost()));
                    g.text(font, c, x + 340 - 56 - font.width(c), y + 9, 0xFFFFCC99, false);
                }
                Component state = Component.translatable(a.enabled ? "morph.gui.on" : "morph.gui.off");
                int bx = x + 340 - 50;
                boolean hover = mx >= bx && mx < bx + 46 && my >= y + 3 && my < y + 23;
                g.fill(bx, y + 3, bx + 46, y + 23, a.enabled ? (hover ? 0xFF3C8C3C : 0xFF2C6C2C) : (hover ? 0xFF6C3C3C : 0xFF4C2C2C));
                g.centeredText(font, state, bx + 23, y + 9, 0xFFFFFFFF);
                abilityRows.add(new AbilityRow(t, y));
            }
            else
            {
                Component passive = Component.translatable("morph.gui.passive");
                g.text(font, passive, x + 340 - 6 - font.width(passive), y + 9, 0xFF77AA77, false);
            }
            y += 30;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt)
    {
        super.extractRenderState(g, mx, my, dt);
        g.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);

        double max = Upgrades.maxBiomass(ClientState.DATA);
        double cur = ClientState.DATA.biomass;
        int bw = 180, bx = width / 2 - bw / 2;
        g.fill(bx - 1, 18, bx + bw + 1, 27, 0xFF000000);
        g.fill(bx, 19, bx + bw, 26, 0xFF2A1A1A);
        g.fill(bx, 19, bx + (int)(bw * Math.min(1D, cur / max)), 26, 0xFFB03A2E);
        g.centeredText(font, Component.translatable("morph.gui.biomass.amount", fmt(Math.floor(cur * 10) / 10), fmt(max)), width / 2, 18, 0xFFFFFFFF);

        if(!ClientState.usesBiomass())
        {
            g.centeredText(font, Component.translatable("morph.gui.biomass.notInThisMode", ClientState.mode), width / 2, 70, 0xFFFFAA55);
        }
        else if(!ClientState.biomassUnlocked)
        {
            g.centeredText(font, Component.translatable("morph.gui.biomass.locked"), width / 2, 70, 0xFFFFAA55);
        }
        switch(tab)
        {
            case UPGRADES -> renderTree(g, mx, my);
            case ABILITIES -> renderAbilities(g, mx, my);
            case MORPHS -> grid.render(g, font, mx, my, dt);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent ev, boolean dbl)
    {
        return clicked(ev, dbl, ev.x(), ev.y(), ev.button());
    }

    private boolean clicked(net.minecraft.client.input.MouseButtonEvent ev, boolean dbl, double mx, double my, int button)
    {
        if(super.mouseClicked(ev, dbl))
        {
            return true;
        }
        if(tab == Tab.MORPHS)
        {
            return grid.mouseClicked((int)mx, (int)my, button);
        }
        if(tab == Tab.UPGRADES && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && my > 58 && my < height - 34)
        {
            for(BiomassUpgradeInfo info : Upgrades.TREE.values())
            {
                int[] p = nodePos.get(info.id);
                if(p == null)
                {
                    continue;
                }
                int nx = originX() + p[0], ny = originY() + p[1];
                if(mx >= nx && mx < nx + NODE_W && my >= ny && my < ny + NODE_H)
                {
                    if(Upgrades.canPurchase(ClientState.DATA, info.id))
                    {
                        Platform.sendToServer(new Packets.PurchaseUpgrade(info.id));
                        minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.2F));
                    }
                    return true;
                }
            }
        }
        if(tab == Tab.ABILITIES && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)
        {
            int x = Math.max(20, width / 2 - 170);
            for(AbilityRow row : abilityRows)
            {
                int bx = x + 340 - 50;
                if(mx >= bx && mx < bx + 46 && my >= row.y + 3 && my < row.y + 23)
                {
                    if(ClientState.usesBiomass())
                    {
                        Platform.sendToServer(new Packets.ToggleAbility(row.trait.type));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent ev, double dx, double dy)
    {
        double mx = ev.x(), my = ev.y();
        int button = ev.button();
        if(tab == Tab.UPGRADES && button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && my > 58 && my < height - 34)
        {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(ev, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy)
    {
        if(tab == Tab.MORPHS && grid.mouseScrolled(mx, my, dy))
        {
            return true;
        }
        if(tab == Tab.UPGRADES)
        {
            panY += dy * 12;
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent ev)
    {
        if(ev.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && grid != null && grid.closePopup())
        {
            return true;
        }
        return super.keyPressed(ev);
    }
}
