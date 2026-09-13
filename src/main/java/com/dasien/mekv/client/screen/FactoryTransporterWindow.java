package com.dasien.mekv.client.screen;

import com.dasien.mekv.factory.RelativeSide;
import com.dasien.mekv.menu.FactoryMenu;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.BasicColorButton;
import mekanism.client.gui.element.button.ColorButton;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.text.BooleanStateDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class FactoryTransporterWindow extends CompatGuiWindow {
    private final FactoryMenu menu;
    private final IntConsumer click;

    public FactoryTransporterWindow(IGuiWrapper gui, int x, int y, FactoryMenu menu, IntConsumer click) {
        super(gui, x, y, 156, 119, SelectedWindowData.WindowType.TRANSPORTER_CONFIG);
        this.menu = menu;
        this.click = click;
        interactionStrategy = InteractionStrategy.ALL;

        addChild(new GuiInnerScreen(gui, relativeX + 41, relativeY + 15, 74, 12, this::strictInputText));
        addChild(new GuiSlot(SlotType.NORMAL, gui, relativeX + 111, relativeY + 48));
        addChild(new MekanismImageButton(gui, relativeX + 136, relativeY + 6, 14, 16, MekGui.EXCLAMATION,
                (element, mouseX, mouseY) -> { click.accept(FactoryMenu.BUTTON_STRICT_INPUT); return true; }));
        addChild(new ColorButton(gui, relativeX + 112, relativeY + 49, 16, 16, menu::getOutputColor,
                 (element, mx, my) -> { click.accept(FactoryMenu.BUTTON_OUTPUT_COLOR_NEXT); return true; },
                 (element, mx, my) -> { click.accept(FactoryMenu.BUTTON_OUTPUT_COLOR_PREV); return true; }));

        addSideButton(RelativeSide.BOTTOM, 41, 80);
        addSideButton(RelativeSide.TOP, 41, 34);
        addSideButton(RelativeSide.FRONT, 41, 57);
        addSideButton(RelativeSide.BACK, 18, 80);
        addSideButton(RelativeSide.LEFT, 18, 57);
        addSideButton(RelativeSide.RIGHT, 64, 57);
    }

    private void addSideButton(RelativeSide side, int x, int y) {
        addChild(new BasicColorButton(gui(), relativeX + x, relativeY + y, 22, () -> color(side),
                (element, mouseX, mouseY) -> { click.accept(FactoryMenu.BUTTON_INPUT_COLOR_NEXT_START + side.ordinal()); return true; },
                (element, mouseX, mouseY) -> { click.accept(FactoryMenu.BUTTON_INPUT_COLOR_PREV_START + side.ordinal()); return true; }));
    }

    private List<Component> strictInputText() {
        return Collections.singletonList(MekanismLang.STRICT_INPUT_ENABLED.translate(
                BooleanStateDisplay.OnOff.of(menu.hasStrictInput())));
    }

    private DataType dataType(RelativeSide side) {
        return switch (menu.getFactory().getSideMode(side)) {
            case NONE, ENERGY -> DataType.NONE;
            case INPUT -> DataType.INPUT;
            case OUTPUT -> DataType.OUTPUT;
            case INPUT_OUTPUT -> DataType.INPUT_OUTPUT;
        };
    }

    private EnumColor color(RelativeSide side) {
        EnumColor configured = menu.getInputColor(side);
        return configured == null ? dataType(side).getColor() : configured;
    }

    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, MekanismLang.TRANSPORTER_CONFIG.translate(), 5);
        drawCenteredText(graphics, MekanismLang.INPUT.translate(), relativeX + 51, relativeY + 105, subheadingTextColor());
        drawCenteredText(graphics, MekanismLang.OUTPUT.translate(), relativeX + 121, relativeY + 68, subheadingTextColor());
    }

    @Override
    protected int getTitlePadEnd() {
        return super.getTitlePadEnd() + 15;
    }
}















