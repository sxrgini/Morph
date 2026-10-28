package com.iiampanda.morph.client.gui;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.client.ClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Scrollable grid of the morphs a player owns. Shared by the selector and the Biomass menu. */
public class MorphGrid
{
    public static final int TILE_W = 48;
    public static final int TILE_H = 64;

    public int x, y, w, h;
    public String filter = "";
    public boolean enabled = true;
    private int scrollRows;

    public Consumer<MorphVariant> onSelect = v -> {};
    public Consumer<MorphVariant> onFavourite = v -> {};
    public Consumer<MorphVariant> onForget = v -> {};

    public MorphGrid(int x, int y, int w, int h)
    {
        setBounds(x, y, w, h);
    }

    public void setBounds(int x, int y, int w, int h)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /** All the forms of one mob type, shown as a single tile. */
    public static final class Group
    {
        public final net.minecraft.resources.ResourceLocation type;
        public final List<MorphVariant> variants = new ArrayList<>();

        Group(net.minecraft.resources.ResourceLocation type)
        {
            this.type = type;
        }

        public boolean anyFavourite()
        {
            for(MorphVariant v : variants)
            {
                if(ClientState.DATA.isFavourite(v))
                {
                    return true;
                }
            }
            return false;
        }

        public Component name()
        {
            net.minecraft.world.entity.EntityType<?> t = variants.get(0).entityType();
            return t == null ? Component.literal(type.toString()) : t.getDescription();
        }

        /** The form drawn on the tile: your current one if it is in here, otherwise the first adult form. */
        MorphVariant representative(MorphVariant current)
        {
            if(current != null && variants.contains(current))
            {
                return current;
            }
            for(MorphVariant v : variants)
            {
                if(!v.baby)
                {
                    return v;
                }
            }
            return variants.get(0);
        }
    }

    private Group popup;
    private int anchorX, anchorY, anchorW; // the tile the popup was opened from
    private int popScroll;

    private int columns()
    {
        return Math.max(1, w / TILE_W);
    }

    private int visibleRows()
    {
        return Math.max(1, h / TILE_H);
    }

    public List<Group> groups()
    {
        java.util.LinkedHashMap<net.minecraft.resources.ResourceLocation, Group> map = new java.util.LinkedHashMap<>();
        String f = filter.toLowerCase(Locale.ROOT);
        for(MorphVariant v : ClientState.DATA.morphs)
        {
            if(f.isEmpty() || v.displayName().getString().toLowerCase(Locale.ROOT).contains(f) || v.type.toString().contains(f) || v.variantLabel().toLowerCase(Locale.ROOT).contains(f))
            {
                map.computeIfAbsent(v.type, Group::new).variants.add(v);
            }
        }
        List<Group> list = new ArrayList<>(map.values());
        for(Group g : list)
        {
            g.variants.sort(Comparator.<MorphVariant, Boolean>comparing(v -> !ClientState.DATA.isFavourite(v)).thenComparing(v -> v.baby).thenComparing(v -> v.variantLabel()));
        }
        list.sort(Comparator.<Group, Boolean>comparing(g -> !g.anyFavourite()).thenComparing(g -> g.name().getString()));
        return list;
    }

    private int maxScroll(int count)
    {
        int rows = (count + columns() - 1) / columns();
        return Math.max(0, rows - visibleRows());
    }

    private int gridLeft()
    {
        return x + (w - columns() * TILE_W) / 2;
    }

    public Group hover(int mx, int my, List<Group> entries)
    {
        if(mx < x || my < y || mx >= x + w || my >= y + h)
        {
            return null;
        }
        int col = (mx - gridLeft()) / TILE_W;
        int row = (my - y) / TILE_H + scrollRows;
        if(mx < gridLeft() || col < 0 || col >= columns())
        {
            return null;
        }
        int idx = row * columns() + col;
        return idx >= 0 && idx < entries.size() ? entries.get(idx) : null;
    }

    //---------------- popup geometry ----------------
    private int popCols()
    {
        return Math.max(1, Math.min(popup.variants.size(), 6));
    }

    private int popRowsVisible()
    {
        return Math.min(3, (popup.variants.size() + popCols() - 1) / popCols());
    }

    private int[] popupRect()
    {
        Minecraft mc = Minecraft.getInstance();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        int pw = popCols() * TILE_W + 8;
        int ph = 16 + popRowsVisible() * TILE_H + 8;
        int px = anchorX + anchorW + 4;
        if(px + pw > sw - 4)
        {
            px = anchorX - 4 - pw;
        }
        px = Math.max(4, px);
        int py = Math.max(4, Math.min(anchorY, sh - 4 - ph));
        return new int[]{px, py, pw, ph};
    }

    private int popMaxScroll()
    {
        int rows = (popup.variants.size() + popCols() - 1) / popCols();
        return Math.max(0, rows - popRowsVisible());
    }

    private MorphVariant popupHover(int mx, int my)
    {
        int[] r = popupRect();
        int gx = r[0] + 4, gy = r[1] + 16;
        if(mx < gx || my < gy || mx >= gx + popCols() * TILE_W || my >= gy + popRowsVisible() * TILE_H)
        {
            return null;
        }
        int idx = ((my - gy) / TILE_H + popScroll) * popCols() + (mx - gx) / TILE_W;
        return idx >= 0 && idx < popup.variants.size() ? popup.variants.get(idx) : null;
    }

    private boolean inPopup(int mx, int my)
    {
        if(popup == null)
        {
            return false;
        }
        int[] r = popupRect();
        return mx >= r[0] && my >= r[1] && mx < r[0] + r[2] && my < r[1] + r[3];
    }

    /** @return true if a popup was open and has been closed (for Escape handling) */
    public boolean closePopup()
    {
        if(popup != null)
        {
            popup = null;
            return true;
        }
        return false;
    }

    //---------------- rendering ----------------
    private static final int STAR = 11;
    private static final net.minecraft.resources.ResourceLocation STAR_OFF = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/star.png");
    private static final net.minecraft.resources.ResourceLocation STAR_ON = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("morph", "textures/gui/star_filled.png");

    /** True if the point is on the favourite star in the top right corner of the tile at (tx, ty). */
    private static boolean overStar(int tx, int ty, int mx, int my)
    {
        int sx = tx + TILE_W - STAR - 4, sy = ty + 4;
        return mx >= sx - 1 && mx < sx + STAR + 1 && my >= sy - 1 && my < sy + STAR + 1;
    }

    /** Stars a mob type (all of its forms) when none is a favourite, takes every favourite form off otherwise. */
    private void toggleGroup(Group gr)
    {
        boolean any = gr.anyFavourite();
        for(MorphVariant v : new ArrayList<>(gr.variants))
        {
            if(ClientState.DATA.isFavourite(v) == any)
            {
                onFavourite.accept(v);
            }
        }
    }

    private static Component starTip(boolean fav)
    {
        return Component.translatable(fav ? "morph.gui.favourite.remove" : "morph.gui.favourite.add");
    }

    private void drawTile(GuiGraphics g, Font font, int tx, int ty, MorphVariant v, boolean isHover, boolean isCurrent, boolean fav, int count, Minecraft mc, float dt)
    {
        g.fill(tx + 2, ty + 2, tx + TILE_W - 2, ty + TILE_H - 2, isHover ? 0xCC4A4A60 : 0xCC26263A);
        if(isCurrent)
        {
            g.renderOutline(tx + 2, ty + 2, TILE_W - 4, TILE_H - 4, 0xFF55FF55);
        }
        LivingEntity e = ClientState.guiEntity(v);
        if(e != null)
        {
            drawEntity(g, e, tx + TILE_W / 2, ty + TILE_H / 2 + 4, isHover, mc, dt);
        }
        g.blit(fav ? STAR_ON : STAR_OFF, tx + TILE_W - STAR - 4, ty + 4, 0, 0, STAR, STAR, STAR, STAR);
        if(v.baby && count <= 1)
        {
            g.drawString(font, "b", tx + 5, ty + 5, 0xFF9AD0FF, true);
        }
        if(count > 1)
        {
            String c = "x" + count;
            g.drawString(font, c, tx + TILE_W - 5 - font.width(c), ty + TILE_H - 13, 0xFFFFFFFF, true);
        }
    }

    public void render(GuiGraphics g, Font font, int mx, int my, float dt)
    {
        Minecraft mc = Minecraft.getInstance();
        List<Group> entries = groups();
        if(popup != null)
        {
            // the group may have changed (forgotten morphs, filter)
            Group fresh = null;
            for(Group gr : entries)
            {
                if(gr.type.equals(popup.type))
                {
                    fresh = gr;
                }
            }
            popup = fresh;
            if(popup != null)
            {
                popScroll = Math.min(popScroll, popMaxScroll());
            }
        }
        boolean popHover = inPopup(mx, my);
        scrollRows = Math.min(scrollRows, maxScroll(entries.size()));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xCC101018);

        if(entries.isEmpty())
        {
            g.drawCenteredString(font, Component.translatable(ClientState.DATA.morphs.isEmpty() ? "morph.gui.grid.empty" : "morph.gui.grid.noResults"), x + w / 2, y + h / 2 - 4, 0xAAAAAA);
            return;
        }

        Group hovered = popup != null ? null : hover(mx, my, entries);
        g.enableScissor(x, y, x + w, y + h);
        int cols = columns();
        int left = gridLeft();
        MorphVariant current = MorphApi.getMorphInfo(mc.player).variant;
        for(int i = scrollRows * cols; i < entries.size() && i < (scrollRows + visibleRows() + 1) * cols; i++)
        {
            Group gr = entries.get(i);
            int tx = left + (i % cols) * TILE_W;
            int ty = y + (i / cols - scrollRows) * TILE_H;
            MorphVariant rep = gr.representative(current);
            boolean isCurrent = current != null && gr.variants.contains(current);
            boolean fav = gr.variants.size() == 1 ? ClientState.DATA.isFavourite(rep) : gr.anyFavourite();
            drawTile(g, font, tx, ty, rep, gr == hovered || (popup == gr), isCurrent, fav, gr.variants.size(), mc, dt);
        }
        g.disableScissor();

        if(hovered != null)
        {
            List<Component> tip = new ArrayList<>();
            int hi = entries.indexOf(hovered);
            if(overStar(left + (hi % cols) * TILE_W, y + (hi / cols - scrollRows) * TILE_H, mx, my))
            {
                tip.add(starTip(hovered.anyFavourite()));
            }
            else
            {
                tip.add(hovered.variants.size() == 1 ? hovered.variants.get(0).displayName() : hovered.name());
            }
            g.renderComponentTooltip(font, tip, mx, my);
        }

        if(popup != null)
        {
            renderPopup(g, font, mx, my, dt, mc, current, popHover);
        }
    }

    private void renderPopup(GuiGraphics g, Font font, int mx, int my, float dt, Minecraft mc, MorphVariant current, boolean popHover)
    {
        int[] r = popupRect();
        g.pose().pushPose();
        g.pose().translate(0F, 0F, 200F);
        g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, 0xFF000000);
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0xFF101018);
        g.drawString(font, popup.name(), r[0] + 5, r[1] + 5, 0xFFFFFFFF, true);
        int gx = r[0] + 4, gy = r[1] + 16;
        g.enableScissor(gx, gy, gx + popCols() * TILE_W, gy + popRowsVisible() * TILE_H);
        MorphVariant hov = popHover ? popupHover(mx, my) : null;
        int cols = popCols();
        for(int i = popScroll * cols; i < popup.variants.size() && i < (popScroll + popRowsVisible()) * cols; i++)
        {
            MorphVariant v = popup.variants.get(i);
            int tx = gx + (i % cols) * TILE_W;
            int ty = gy + (i / cols - popScroll) * TILE_H;
            drawTile(g, font, tx, ty, v, v == hov, v.equals(current), ClientState.DATA.isFavourite(v), 1, mc, dt);
        }
        g.disableScissor();
        if(hov != null)
        {
            List<Component> tip = new ArrayList<>();
            int pi = popup.variants.indexOf(hov);
            if(overStar(gx + (pi % cols) * TILE_W, gy + (pi / cols - popScroll) * TILE_H, mx, my))
            {
                tip.add(starTip(ClientState.DATA.isFavourite(hov)));
            }
            else
            {
                tip.add(hov.displayName());
                String label = hov.variantLabel();
                if(!label.isEmpty())
                {
                    tip.add(Component.literal(label).withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            g.pose().translate(0F, 0F, 200F);
            g.renderComponentTooltip(font, tip, mx, my);
        }
        g.pose().popPose();
    }

    private static final java.util.Set<String> LOGGED_GUI_FAILURES = new java.util.HashSet<>();

    private void drawEntity(GuiGraphics g, LivingEntity e, int cx, int centerY, boolean spin, Minecraft mc, float dt)
    {
        // models (panda body, squid tentacles...) stick out past their hitbox, so leave some margin inside the tile
        float size = Math.max(e.getBbHeight(), e.getBbWidth()) * 1.3F;
        float scale = Math.max(3F, Math.min(28F, 32F / Math.max(0.3F, size)));
        float angle = spin ? (mc.level.getGameTime() + dt) * 4F : 20F;
        e.tickCount = mc.player.tickCount;
        e.yBodyRot = e.yBodyRotO = 180F + angle;
        e.setYRot(180F + angle);
        e.yRotO = 180F + angle;
        e.setXRot(0F);
        e.xRotO = 0F;
        e.yHeadRot = e.yHeadRotO = 180F + angle;
        Quaternionf pose = new Quaternionf().rotateZ((float)Math.PI);
        pose.mul(new Quaternionf().rotateX(-10F * ((float)Math.PI / 180F)));
        try
        {
            InventoryScreen.renderEntityInInventory(g, cx, centerY, scale, new Vector3f(0F, e.getBbHeight() / 2F + 0.0625F, 0F), pose, null, e);
        }
        catch(Throwable t)
        {
            // some modded renderers don't like being drawn in menus; skip them
            if(LOGGED_GUI_FAILURES.add(e.getType().toString()))
            {
                com.iiampanda.morph.common.Morph.LOGGER.warn("Could not draw {} in the morph menu", e.getType(), t);
            }
        }
    }

    public boolean mouseClicked(int mx, int my, int button)
    {
        if(popup != null)
        {
            if(inPopup(mx, my))
            {
                MorphVariant v = popupHover(mx, my);
                if(v != null)
                {
                    int[] pr = popupRect();
                    int pi = popup.variants.indexOf(v);
                    if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && overStar(pr[0] + 4 + (pi % popCols()) * TILE_W, pr[1] + 16 + (pi / popCols() - popScroll) * TILE_H, mx, my))
                    {
                        onFavourite.accept(v);
                        return true;
                    }
                    if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && enabled)
                    {
                        popup = null;
                        onSelect.accept(v);
                    }
                    else if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)
                    {
                        if(net.minecraft.client.gui.screens.Screen.hasShiftDown())
                        {
                            onForget.accept(v);
                        }
                        else
                        {
                            onFavourite.accept(v);
                        }
                    }
                }
                return true;
            }
            popup = null; // clicked elsewhere: close, and let the click act on whatever is underneath
        }
        List<Group> entries = groups();
        Group gr = hover(mx, my, entries);
        if(gr == null)
        {
            return false;
        }
        int sidx = entries.indexOf(gr);
        if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && overStar(gridLeft() + (sidx % columns()) * TILE_W, y + (sidx / columns() - scrollRows) * TILE_H, mx, my))
        {
            toggleGroup(gr);
            return true;
        }
        if(gr.variants.size() == 1)
        {
            MorphVariant v = gr.variants.get(0);
            if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && enabled)
            {
                onSelect.accept(v);
            }
            else if(button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)
            {
                if(net.minecraft.client.gui.screens.Screen.hasShiftDown())
                {
                    onForget.accept(v);
                }
                else
                {
                    onFavourite.accept(v);
                }
            }
            return true;
        }
        // several forms: open the variant picker next to the tile
        int idx = entries.indexOf(gr);
        anchorX = gridLeft() + (idx % columns()) * TILE_W;
        anchorY = y + (idx / columns() - scrollRows) * TILE_H;
        anchorW = TILE_W;
        popScroll = 0;
        popup = gr;
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double dy)
    {
        if(popup != null && inPopup((int)mx, (int)my))
        {
            popScroll = Math.max(0, Math.min(popMaxScroll(), popScroll - (int)Math.signum(dy)));
            return true;
        }
        if(mx < x || my < y || mx >= x + w || my >= y + h)
        {
            return false;
        }
        scrollRows = Math.max(0, Math.min(maxScroll(groups().size()), scrollRows - (int)Math.signum(dy)));
        return true;
    }
}
