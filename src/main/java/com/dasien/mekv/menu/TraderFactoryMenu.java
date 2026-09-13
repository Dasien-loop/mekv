package com.dasien.mekv.menu;

import com.dasien.mekv.blockentity.TraderFactoryBlockEntity;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.inventory.FilteredSlot;
import com.dasien.mekv.inventory.LockedSlot;
import com.dasien.mekv.inventory.OutputSlot;
import com.dasien.mekv.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;
import com.dasien.mekv.network.FactoryNetwork;
import net.minecraft.server.level.ServerPlayer;

public class TraderFactoryMenu extends FactoryMenu {
    @Nullable
    private MerchantOffers syncedTradeOffers;
    // The actual upgrade UI is supplied by FactoryUpgradeWindow. Keeping the
    // backing container slots out of this dense process layout prevents them
    // from receiving clicks intended for the ultimate tier's rightmost lanes.
    private static final int HIDDEN_UPGRADE_SLOT_COORDINATE = -1_000;
    /** Header layout on the standard 214 px factory canvas. */
    public static final int VILLAGER_X = 20;
    public static final int VILLAGER_Y = 16;
    public static final int WORKSTATION_X = 40;
    public static final int WORKSTATION_Y = 16;
    public static final int TRADE_STATUS_X = 64;
    public static final int COST_A_X = 133;
    public static final int COST_B_X = 151;
    public static final int RESULT_X = 178;
    /** Global trade preview row (cost A, cost B, result). */
    public static final int TRADE_Y = 16;
    public static final int GLOBAL_CONTROL_X = 106;
    /** The global pause control shares the header row with the trade preview. */
    public static final int GLOBAL_CONTROL_Y = TRADE_Y;
    // Keep the lane controls below the global preview controls. This avoids
    // overlap when the compact tiers place columns near the center of the GUI.
    public static final int PROCESS_TRADE_BUTTON_Y = 48;
    public static final int PROCESS_INPUT_Y = 64;
    public static final int PROCESS_PROGRESS_Y = 84;
    public static final int PROCESS_OUTPUT_Y = 108;
    public static final int PROCESS_PAUSE_Y = 128;
    /** Reserve a fixed left gutter for the energy slot and vertical power bar. */
    private static final int PROCESS_GRID_LEFT = ENERGY_SLOT_X + 25;
    private static final int PROCESS_GRID_RIGHT = 8;

    public static int processSpacing(FactoryTier tier) {
        return tier == FactoryTier.BASIC ? 38 : tier == FactoryTier.ADVANCED ? 26 : 19;
    }

    public int processX(int process) {
        int processCount = factory.getTier().processes();
        int spacing = processSpacing(factory.getTier());
        int laneWidth = 18 + Math.max(0, processCount - 1) * spacing;
        int contentWidth = Math.max(0, guiWidth() - PROCESS_GRID_LEFT - PROCESS_GRID_RIGHT);
        int firstLaneX = PROCESS_GRID_LEFT + Math.max(0, (contentWidth - laneWidth) / 2);
        return firstLaneX + Math.max(0, Math.min(process, processCount - 1)) * spacing;
    }

    /** Centers the fixed-width header above the variable-width process grid. */
    public int headerOffset() {
        return (guiWidth() - FactoryTier.ULTIMATE.guiWidth()) / 2;
    }

    public int headerX(int baseX) {
        return baseX + headerOffset();
    }
    public int processProgressX(int process) {
        return processX(process) + 4;
    }

    public TraderFactoryMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, getFactory(playerInv, buf, TraderFactoryBlockEntity.class));
    }

    public TraderFactoryMenu(int id, Inventory playerInv, TraderFactoryBlockEntity factory) {
        super(ModMenus.TRADER_FACTORY.get(), id, playerInv, factory, energyData(factory));
    }

    @Override
    public void setSynchronizer(net.minecraft.world.inventory.ContainerSynchronizer synchronizer) {
        super.setSynchronizer(synchronizer);
        if (menuPlayer instanceof ServerPlayer player) {
            FactoryNetwork.sendTradeOffers(player, this, getTrader().getOffersForSync());
        }
    }

    public void setSyncedTradeOffers(MerchantOffers offers) {
        syncedTradeOffers = offers == null ? null : offers.copy();
    }

    public TraderFactoryBlockEntity getTrader() {
        return (TraderFactoryBlockEntity) factory;
    }

    @Override
    protected void addMachineSlots() {
        addVillagerSlot(headerX(VILLAGER_X), VILLAGER_Y);
        addSlot(new FilteredSlot(getTrader().getWorkstationHandler(), 0, headerX(WORKSTATION_X), WORKSTATION_Y, stack ->
                stack.getItem() instanceof BlockItem blockItem && getTrader().isValidWorkstation(blockItem.getBlock())));
        addProcessColumns();
        ItemStackHandler display = new ItemStackHandler(3) {
            @Override
            public ItemStack getStackInSlot(int slot) {
                return switch (slot) {
                    case 0 -> displayCostA();
                    case 1 -> displayCostB();
                    case 2 -> displayResult();
                    default -> ItemStack.EMPTY;
                };
            }
        };
        addSlot(new LockedSlot(display, 0, headerX(COST_A_X), TRADE_Y));
        addSlot(new LockedSlot(display, 1, headerX(COST_B_X), TRADE_Y));
        addSlot(new LockedSlot(display, 2, headerX(RESULT_X), TRADE_Y));
    }

    private void addProcessColumns() {
        int processCount = factory.getTier().processes();
        for (int process = 0; process < processCount; process++) {
            int x = processX(process);
            final int lane = process;
            ItemStackHandler display = new ItemStackHandler(1) {
                @Override
                public ItemStack getStackInSlot(int slot) {
                    return slot == 0 ? getProcessResult(lane) : ItemStack.EMPTY;
                }
            };
            addSlot(new LockedSlot(display, 0, x, PROCESS_TRADE_BUTTON_Y));
            addSlot(new com.dasien.mekv.inventory.FactorySlot(factory.getInputItems(), process, x, PROCESS_INPUT_Y) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    // Slot#setChanged is used by vanilla quick-move when it
                    // mutates an existing live ItemStack without invoking the
                    // handler's onContentsChanged callback.
                    getTrader().markInputDirty();
                }
            });
            addSlot(new OutputSlot(factory.getOutputItems(), process, x, PROCESS_OUTPUT_Y) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    // Moving an output stack to the player mutates the live
                    // handler stack before Slot#setChanged is called. Flush
                    // that mutation so it is saved and synced immediately.
                    getTrader().markOutputDirty();
                }
            });
        }
    }

    /**
     * Move player stacks only through the real trade input handler. The base
     * factory range also contains preview/locked slots, and vanilla quick
     * move can report success after a partial move without preserving the
     * remainder when a handler owns copied stacks.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int playerStart = slots.size() - 36;
        if (index < playerStart) {
            return super.quickMoveStack(player, index);
        }
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack live = source.getItem();
        ItemStack original = live.copy();
        if (getTrader().isSupportedUpgrade(live) && isUpgradeSlotActive()) {
            return super.quickMoveStack(player, index);
        }
        if (getTrader().isValidEnergyItem(live)) {
            return super.quickMoveStack(player, index);
        }
        // Leave villagers and workstations (and any other non-trade item) to
        // the normal machine-slot routing. This path is only for trade inputs.
        boolean acceptedByLane = false;
        for (int process = 0; process < getTrader().getInputItems().getSlots(); process++) {
            if (getTrader().getInputItems().isItemValid(process, live)) {
                acceptedByLane = true;
                break;
            }
        }
        if (getTrader().getInputItems().getSlots() == 0 || !acceptedByLane) {
            return super.quickMoveStack(player, index);
        }

        ItemStack remaining = live.copy();
        int before = remaining.getCount();
        for (int process = 0; process < getTrader().getInputItems().getSlots() && !remaining.isEmpty(); process++) {
            remaining = getTrader().getInputItems().insertItem(process, remaining, false);
        }
        int moved = before - remaining.getCount();
        if (moved <= 0) {
            return ItemStack.EMPTY;
        }
        if (moved > original.getCount() || remaining.getCount() < 0) {
            return ItemStack.EMPTY;
        }
        live.shrink(moved);
        source.setChanged();
        getTrader().markInputDirty();
        return original;
    }
    private ItemStack displayCostA() {
        MerchantOffer offer = getSelectedOffer();
        return offer == null ? ItemStack.EMPTY : offer.getCostA().copy();
    }

    private ItemStack displayCostB() {
        MerchantOffer offer = getSelectedOffer();
        return offer == null ? ItemStack.EMPTY : offer.getCostB().copy();
    }

    private ItemStack displayResult() {
        MerchantOffer offer = getSelectedOffer();
        return offer == null ? ItemStack.EMPTY : offer.getResult().copy();
    }

    @Nullable
    public MerchantOffer getSelectedOffer() {
        MerchantOffers offers = getTradeOffers();
        if (offers == null) return null;
        int index = getTradeIndex();
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }

    @Nullable
    public MerchantOffer getProcessOffer(int process) {
        if (process < 0 || process >= getProcesses()) {
            return null;
        }
        MerchantOffers offers = getTradeOffers();
        if (offers == null) return null;
        int index = getSlotTradeIndex(process);
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }

    @Nullable
    public MerchantOffers getTradeOffers() {
        if (menuPlayer.level().isClientSide) {
            return syncedTradeOffers;
        }
        if (menuPlayer.level().isClientSide) {
            return syncedTradeOffers;
        }
        var entity = getTrader().getVillagerEntity();
        if (entity == null) return null;
        try {
            return entity.getOffers();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public ItemStack getProcessResult(int process) {
        MerchantOffer offer = getProcessOffer(process);
        return offer == null ? ItemStack.EMPTY : offer.getResult().copy();
    }

    public int getProcessTradeUses(int process) {
        return getTrader().getTradeUses(process);
    }

    public int getProcessTradeMaxUses(int process) {
        return getTrader().getTradeMaxUses(process);
    }

    /** Uses synced menu slots, including large-stack packets, rather than a stale client block entity. */
    public boolean isTradeSelectionLocked(int process) {
        if (process < 0) {
            return false;
        }
        if (process >= getProcesses() || getSlotProgress(process) > 0) {
            return true;
        }
        for (Slot slot : slots) {
            if (slot.x == processX(process) && slot.y == PROCESS_INPUT_Y) {
                return slot.hasItem();
            }
        }
        return true;
    }

    @Override
    public int playerInventoryY() {
        // Keep the standard inventory close to the machine area. The process
        // count changes the width, not the height, just as in Mekanism's GUI.
        return PROCESS_PAUSE_Y + 22;
    }

    @Override
    public int guiWidth() {
        // The global controls and the ultimate tier's nine columns need the
        // wider Mekanism factory canvas. Using one width for all tiers also
        // keeps the inventory and side tabs from jumping when a block is
        // upgraded.
        return factory.getTier().guiWidth();
    }

    @Override
    protected int upgradeSlotX() {
        return HIDDEN_UPGRADE_SLOT_COORDINATE;
    }

    @Override
    protected int upgradeSlotInputY() {
        return HIDDEN_UPGRADE_SLOT_COORDINATE;
    }

    @Override
    protected int upgradeSlotOutputY() {
        return HIDDEN_UPGRADE_SLOT_COORDINATE;
    }

    public boolean isGlobalTradeSlot(Slot slot) {
        return slot instanceof LockedSlot && slot.y == TRADE_Y
                && (slot.x == headerX(COST_A_X) || slot.x == headerX(COST_B_X)
                || slot.x == headerX(RESULT_X));
    }

    public int getProcessTradeSlot(Slot slot) {
        if (!(slot instanceof LockedSlot) || slot.y != PROCESS_TRADE_BUTTON_Y) {
            return -1;
        }
        for (int process = 0; process < getProcesses(); process++) {
            if (slot.x == processX(process)) {
                return process;
            }
        }
        return -1;
    }
}












