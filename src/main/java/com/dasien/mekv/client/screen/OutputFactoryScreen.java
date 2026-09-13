package com.dasien.mekv.client.screen;

import com.dasien.mekv.menu.OutputFactoryMenu;
import com.dasien.mekv.factory.VillagerFactoryType;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class OutputFactoryScreen extends FactoryScreen<OutputFactoryMenu> {
    private static final int PROGRESS_X = 75;
    private static final int PROGRESS_Y = 19;

    public OutputFactoryScreen(OutputFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        if (menu.getFactory().getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || menu.getFactory().getFactoryType() == VillagerFactoryType.FARMER) {
            for (int process = 0; process < menu.getProcesses(); process++) {
                final int index = process;
                addRenderableWidget(new GuiProgress(
                        () -> Math.min(1.0, Math.max(0.0,
                                (double) menu.getSlotProgress(index) / menu.getMaxProgress())),
                        ProgressType.DOWN, this, menu.processX(process) + 4, OutputFactoryMenu.IRON_PROGRESS_Y));
            }
            return;
        }
        addRenderableWidget(new GuiInnerScreen(this, 29, 36, 66, 28, this::statusText).tooltip(this::machineInfo));
        addRenderableWidget(new GuiProgress(
                () -> Math.min(1.0, Math.max(0.0, (double) menu.getProgress() / menu.getMaxProgress())),
                ProgressType.SMALL_RIGHT, this, PROGRESS_X, PROGRESS_Y));
    }

    private List<Component> statusText() {
        List<Component> lines = new ArrayList<>();
        lines.add(menu.getStatusText());
        lines.add(menu.getProgressText());
        return lines;
    }
}
