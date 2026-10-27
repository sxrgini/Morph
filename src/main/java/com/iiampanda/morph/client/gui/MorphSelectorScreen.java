package com.iiampanda.morph.client.gui;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.client.ClientState;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class MorphSelectorScreen extends Screen
{
    private MorphGrid grid;
    private EditBox search;

    public MorphSelectorScreen()
    {
        super(Component.translatable("morph.gui.selector.title"));
    }

    @Override
    protected void init()
    {
        int gw = Math.min(width - 40, MorphGrid.TILE_W * 9);
        int gh = Math.min(height - 90, MorphGrid.TILE_H * 4);
        int gx = (width - gw) / 2;
        int gy = 42;
        grid = new MorphGrid(gx, gy, gw, gh);
        grid.enabled = ClientState.canMorph;
        grid.onSelect = v ->
        {
            Platform.sendToServer(new Packets.RequestMorph(Optional.of(v.write())));
            onClose();
        };
        grid.onFavourite = v -> Platform.sendToServer(new Packets.SetFavourite(v.write(), !ClientState.DATA.isFavourite(v)));
        grid.onForget = v -> Platform.sendToServer(new Packets.ForgetMorph(v.write()));

        search = new EditBox(font, gx, 18, Math.min(180, gw), 18, Component.translatable("morph.gui.search"));
        search.setHint(Component.translatable("morph.gui.search"));
        search.setResponder(s -> grid.filter = s);
        addRenderableWidget(search);

        int by = gy + gh + 8;
        addRenderableWidget(Button.builder(Component.translatable("morph.gui.demorph"), b ->
        {
            Platform.sendToServer(new Packets.RequestMorph(Optional.empty()));
            onClose();
        }).bounds(width / 2 - 104, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 + 4, by, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt)
    {
        super.render(g, mx, my, dt); // draws blurred background + widgets once
        g.drawCenteredString(font, title, width / 2, 6, 0xFFFFFF);
        grid.render(g, font, mx, my, dt);
        if(!ClientState.canMorph)
        {
            g.drawCenteredString(font, Component.translatable("morph.gui.cannotMorphNow"), width / 2, grid.y + grid.h + 34, 0xFFAA55);
        }
        MorphVariant cur = MorphApi.getMorphInfo(minecraft.player).variant;
        if(cur != null)
        {
            g.drawString(font, Component.translatable("morph.gui.currently", cur.displayName()), search.getX() + search.getWidth() + 10, 23, 0xAAFFAA);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        if(grid.mouseClicked((int)mx, (int)my, button))
        {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy)
    {
        return grid.mouseScrolled(mx, my, dy) || super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if(keyCode == 256 && grid != null && grid.closePopup())
        {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
