package com.dasien.mekv.menu;

import com.dasien.mekv.blockentity.FarmerFactoryBlockEntity;
import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.inventory.FilteredSlot;
import com.dasien.mekv.inventory.OutputSlot;
import com.dasien.mekv.registry.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class OutputFactoryMenu extends FactoryMenu {
    public static final int VILLAGER_X = 29;
    public static final int VILLAGER_Y = 16;
    public static final int SEED_X = 52;
    public static final int SEED_Y = 16;
    public static final int OUTPUT_X = 106;
    public static final int OUTPUT_Y = 16;
    // GuiProgress.DOWN renders a 20px texture from y=33 through y=52.
    // GuiSlot is drawn one pixel above the container slot coordinate, so keep
    // the first output row at y=55 to leave a visible gap below the arrow.
    public static final int IRON_OUTPUT_TOP_Y = 55;
    public static final int IRON_OUTPUT_BOTTOM_Y = 73;
    // DOWN progress texture is 20px tall. Keep its bottom edge above the
    // first output row, matching Mekanism's factory layout.
    public static final int IRON_PROGRESS_Y = 33;

    public OutputFactoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getFactory(playerInv, buf, VillagerFactoryBlockEntity.class));
    }

    public OutputFactoryMenu(int id, Inventory playerInv, VillagerFactoryBlockEntity factory) {
        super(ModMenus.OUTPUT_FACTORY.get(), id, playerInv, factory, energyData(factory));
    }

    @Override
    protected void addMachineSlots() {
        if (factory.getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || factory.getFactoryType() == VillagerFactoryType.FARMER) {
            int middle = (factory.getTier().processes() - 1) / 2;
            int middleX = processX(middle);
            addVillagerSlot(middleX, VILLAGER_Y);
            if (factory instanceof FarmerFactoryBlockEntity farmer) {
                addSlot(new FilteredSlot(farmer.getSeedHandler(), 0, middleX + 18, SEED_Y,
                        farmer::isValidSeedStack));
            }
            for (int process = 0; process < factory.getTier().processes(); process++) {
                int x = processX(process);
                addSlot(new OutputSlot(factory.getOutputItems(), process * 2, x, IRON_OUTPUT_TOP_Y));
                addSlot(new OutputSlot(factory.getOutputItems(), process * 2 + 1, x, IRON_OUTPUT_BOTTOM_Y));
            }
            return;
        }
        addVillagerSlot(VILLAGER_X, VILLAGER_Y);
        if (factory instanceof FarmerFactoryBlockEntity farmer) {
            addSlot(new FilteredSlot(farmer.getSeedHandler(), 0, SEED_X, SEED_Y, farmer::isValidSeedStack));
        }
        addItemGrid(factory.getOutputItems(), OUTPUT_X, OUTPUT_Y, true);
    }

    @Override
    public int playerInventoryY() {
        if (factory.getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || factory.getFactoryType() == VillagerFactoryType.FARMER) {
            return IRON_OUTPUT_BOTTOM_Y + 36;
        }
        // Leave room for the energy item slot below the power bar before the player inventory.
        return Math.max(98, OUTPUT_Y + gridRows(factory.getOutputItems().getSlots()) * 18 + 18);
    }

    @Override
    public int guiWidth() {
        return factory.getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || factory.getFactoryType() == VillagerFactoryType.FARMER ? factory.getTier().guiWidth() : 176;
    }

    public int processX(int process) {
        return factory.getTier().processX(process);
    }

    /**
     * Returns a progress description whose units match the operation shown in
     * the output factory. Raw crop ages and golem ticks are not meaningful to
     * players without this context.
     */
    public Component getProgressText() {
        int max = Math.max(1, getMaxProgress());
        int progress = Math.max(0, Math.min(getProgress(), max));
        int percent = (int) Math.round(progress * 100.0 / max);
        String percentage = percent + "%";
        if (factory.getFactoryType() == VillagerFactoryType.FARMER) {
            return Component.translatable(
                    "gui.mekv.progress.crop_growth", progress, max, percentage);
        }
        if (factory.getFactoryType() == VillagerFactoryType.IRON_GOLEM) {
            return Component.translatable(
                    "gui.mekv.progress.golem_cycle", ticksToSeconds(progress), ticksToSeconds(max), percentage);
        }
        return Component.translatable("gui.mekv.progress_value", progress, max);
    }

    private static int ticksToSeconds(int ticks) {
        return Math.max(0, (int) Math.round(ticks / 20.0));
    }
}












