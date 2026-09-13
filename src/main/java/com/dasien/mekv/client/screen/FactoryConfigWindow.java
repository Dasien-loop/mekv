package com.dasien.mekv.client.screen;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.RelativeSide;
import com.dasien.mekv.menu.FactoryMenu;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.client.gui.element.button.BasicColorButton;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.text.BooleanStateDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

public class FactoryConfigWindow extends CompatGuiWindow {
    private final FactoryMenu menu;
    private final IntConsumer click;
    private final MekanismButton ejectButton;
    private final ConfigTypeTab itemTab;
    private final ConfigTypeTab energyTab;
    private boolean energy;

    public FactoryConfigWindow(IGuiWrapper gui, int x, int y, FactoryMenu menu, IntConsumer click) {
        super(gui, x, y, 156, 135, SelectedWindowData.WindowType.SIDE_CONFIG);
        this.menu = menu;
        this.click = click;
        interactionStrategy = InteractionStrategy.ALL;

        addChild(new GuiInnerScreen(gui, relativeX + 41, relativeY + 25, 74, 12, this::ejectText).tooltip(this::ejectTooltip));

        itemTab = addChild(new ConfigTypeTab(gui, relativeX - 26, relativeY + 2, false));
        energyTab = addChild(new ConfigTypeTab(gui, relativeX - 26, relativeY + 30, true));
        updateTabs();

        ejectButton = addChild(new MekanismImageButton(gui, relativeX + 136, relativeY + 6, 14, MekGui.AUTO_EJECT,
                (element, mouseX, mouseY) -> {
                    if (!energy) {
                        click.accept(FactoryMenu.BUTTON_EJECT);
                    }
                    return true;
                }));
        addChild(new MekanismImageButton(gui, relativeX + 136, relativeY + 95, 14, 16, MekGui.CLEAR_SIDES,
                (element, mouseX, mouseY) -> { click.accept(energy ? FactoryMenu.BUTTON_CLEAR_ENERGY_SIDES : FactoryMenu.BUTTON_CLEAR_ITEM_SIDES); return true; }));

        addSideButton(RelativeSide.BOTTOM, 68, 92);
        addSideButton(RelativeSide.TOP, 68, 46);
        addSideButton(RelativeSide.FRONT, 68, 69);
        addSideButton(RelativeSide.BACK, 45, 92);
        addSideButton(RelativeSide.LEFT, 45, 69);
        addSideButton(RelativeSide.RIGHT, 91, 69);
        updateEjectButton();
    }

    private void addSideButton(RelativeSide side, int x, int y) {
        addChild(new FactorySideDataButton(gui(), relativeX + x, relativeY + y, side, menu.getFactory(),
                () -> color(side),
                () -> click.accept((energy ? FactoryMenu.BUTTON_ENERGY_SIDE_START : FactoryMenu.BUTTON_SIDE_START) + side.ordinal()),
                () -> {
                    if (energy) {
                        click.accept(FactoryMenu.BUTTON_ENERGY_SIDE_START + side.ordinal());
                    } else {
                        click.accept(FactoryMenu.BUTTON_SIDE_PREV_START + side.ordinal());
                    }
                }));
    }

    void setEnergy(boolean energy) {
        if (this.energy != energy) {
            this.energy = energy;
            updateTabs();
            updateEjectButton();
        }
    }

    private void updateTabs() {
        itemTab.visible = energy;
        energyTab.visible = !energy;
    }

    private void updateEjectButton() {
        ejectButton.active = !energy;
    }

    private List<Component> ejectText() {
        if (energy) {
            return Collections.singletonList(MekanismLang.NO_EJECT.translate());
        }
        return Collections.singletonList(MekanismLang.EJECT.translate(BooleanStateDisplay.OnOff.of(menu.isAutoEject())));
    }

    private List<Component> ejectTooltip() {
        if (energy) {
            return Collections.singletonList(MekanismLang.CANT_EJECT_TOOLTIP.translate());
        }
        return Collections.emptyList();
    }

    private DataType dataType(RelativeSide side) {
        if (energy) {
            return menu.getFactory().isEnergySide(side) ? DataType.INPUT : DataType.NONE;
        }
        return switch (menu.getFactory().getSideMode(side)) {
            case NONE, ENERGY -> DataType.NONE;
            case INPUT -> DataType.INPUT;
            case OUTPUT -> DataType.OUTPUT;
            case INPUT_OUTPUT -> DataType.INPUT_OUTPUT;
        };
    }

    private EnumColor color(RelativeSide side) {
        DataType type = dataType(side);
        return type == null ? EnumColor.GRAY : type.getColor();
    }

    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, MekanismLang.SIDE_CONFIG.translate(), 5);
    }

    @Override
    protected int getTitlePadEnd() {
        return super.getTitlePadEnd() + 15;
    }

    private class ConfigTypeTab extends GuiInsetElement<Void> {
        private final boolean energyTab;

        ConfigTypeTab(IGuiWrapper gui, int x, int y, boolean energyTab) {
            super(MekanismUtils.getResource(MekanismUtils.ResourceType.GUI, type(energyTab).getTransmission() + ".png"),
                    gui, null, x, y, 26, 18, true);
            this.energyTab = energyTab;
        }

        private static TransmissionType type(boolean energyTab) {
            return energyTab ? TransmissionType.ENERGY : TransmissionType.ITEM;
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, energyTab ? SpecialColors.TAB_ENERGY_CONFIG : SpecialColors.TAB_ITEM_CONFIG);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            setEnergy(energyTab);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, TextComponentUtil.build(type(energyTab)));
        }
    }

    private static class FactorySideDataButton extends BasicColorButton {
        private final ItemStack otherBlockItem;

        FactorySideDataButton(IGuiWrapper gui, int x, int y, RelativeSide side, VillagerFactoryBlockEntity factory,
                              java.util.function.Supplier<EnumColor> color, Runnable left, Runnable right) {
            super(gui, x, y, 22, color, (element, mouseX, mouseY) -> { left.run(); return true; }, (element, mouseX, mouseY) -> { right.run(); return true; });
            this.otherBlockItem = neighborItem(factory, side);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.drawBackground(graphics, mouseX, mouseY, partialTick);
            if (!otherBlockItem.isEmpty()) {
                gui().renderItem(graphics, otherBlockItem, getRelativeX() + 3, getRelativeY() + 3, 1.0f);
            }
        }

        private static ItemStack neighborItem(VillagerFactoryBlockEntity factory, RelativeSide side) {
            Level level = factory.getLevel();
            if (level == null) {
                return ItemStack.EMPTY;
            }
            Direction direction = side.toWorld(factory.getFacing());
            BlockPos neighbor = factory.getBlockPos().relative(direction);
            BlockState state = level.getBlockState(neighbor);
            if (state.isAir()) {
                return ItemStack.EMPTY;
            }
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(neighbor).relative(direction.getOpposite(), 0.5d),
                    direction.getOpposite(), neighbor, false);
            return state.getCloneItemStack(hit, level, neighbor, Minecraft.getInstance().player);
        }
    }
}
















