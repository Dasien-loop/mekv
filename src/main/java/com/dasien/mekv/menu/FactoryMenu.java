package com.dasien.mekv.menu;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.FactoryStatus;
import com.dasien.mekv.factory.RedstoneMode;
import com.dasien.mekv.factory.RelativeSide;
import com.dasien.mekv.inventory.FilteredSlot;
import com.dasien.mekv.inventory.OutputSlot;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import mekanism.api.Upgrade;
import mekanism.api.text.EnumColor;
import mekanism.api.security.SecurityMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class FactoryMenu extends AbstractContainerMenu {
    public static final int BUTTON_REDSTONE = 2;
    public static final int BUTTON_EJECT = 3;
    public static final int BUTTON_SIDE_START = 4;
    public static final int BUTTON_ENERGY_SIDE_START = 10;
    public static final int BUTTON_REDSTONE_IGNORED = 20;
    public static final int BUTTON_REDSTONE_HIGH = 21;
    public static final int BUTTON_REDSTONE_LOW = 22;
    public static final int BUTTON_REDSTONE_PREV = 23;
    public static final int BUTTON_CLEAR_ITEM_SIDES = 24;
    public static final int BUTTON_CLEAR_ENERGY_SIDES = 25;
    public static final int BUTTON_SIDE_PREV_START = 26;
    public static final int BUTTON_UNINSTALL_INFINITE = 36;
    public static final int BUTTON_UNINSTALL_INFINITE_ALL = 37;
    public static final int BUTTON_UPGRADE_WINDOW_OPEN = 38;
    public static final int BUTTON_UPGRADE_WINDOW_CLOSE = 39;
    public static final int BUTTON_UNINSTALL_ONE = 40;
    public static final int BUTTON_UNINSTALL_ALL = 50;
    public static final int BUTTON_STRICT_INPUT = 60;
    public static final int BUTTON_OUTPUT_COLOR_NEXT = 61;
    public static final int BUTTON_OUTPUT_COLOR_PREV = 62;
    public static final int BUTTON_OUTPUT_COLOR_CLEAR = 63;
    public static final int BUTTON_INPUT_COLOR_NEXT_START = 64;
    public static final int BUTTON_INPUT_COLOR_PREV_START = 70;
    public static final int BUTTON_INPUT_COLOR_CLEAR_START = 76;
    public static final int BUTTON_SECURITY_NEXT = 82;
    public static final int BUTTON_SECURITY_PREV = 83;
    public static final int BUTTON_AUTO_SORT = 84;
    public static final int BUTTON_TRADE_PAUSE = 86;
    public static final int BUTTON_TRADE_SLOT_PAUSE_START = 120;
    /** Trade selection packets reserve one byte for the offer index. */
    public static final int TRADE_SELECTION_STRIDE = 256;
    public static final int BUTTON_TRADE_SELECT_GLOBAL_START = 1_000;
    public static final int BUTTON_TRADE_SELECT_PROCESS_START = 2_000;
    public static final int UPGRADE_SLOT_X = 144;
    public static final int UPGRADE_SLOT_Y = 34;
    public static final int UPGRADE_SLOT_OUTPUT_Y = 89;
    public static final int ENERGY_SLOT_X = 7;
    // Keep the item slot below the 52px vertical power bar, matching Mekanism's machine layout.
    public static final int ENERGY_SLOT_Y = 72;

    public static int tradeSelectionButton(int process, int tradeIndex) {
        if (tradeIndex < 0 || tradeIndex >= TRADE_SELECTION_STRIDE) {
            return -1;
        }
        return process < 0
                ? BUTTON_TRADE_SELECT_GLOBAL_START + tradeIndex
                : BUTTON_TRADE_SELECT_PROCESS_START + process * TRADE_SELECTION_STRIDE + tradeIndex;
    }

    protected final VillagerFactoryBlockEntity factory;
    protected final ContainerData data;
    protected final List<Slot> upgradeSlots = new ArrayList<>();
    protected Slot energySlot;
    private boolean upgradeWindowOpen;
    private final Player menuPlayer;
    private final ContainerData syncedData;

    protected FactoryMenu(MenuType<?> type, int id, Inventory playerInv, VillagerFactoryBlockEntity factory, ContainerData data) {
        super(type, id);
        this.factory = factory;
        this.data = data;
        this.menuPlayer = playerInv.player;
        this.syncedData = new IntContainerData(data);
        addDataSlots(syncedData);
        addMachineSlots();
        addEnergySlot();
        addUpgradeSlots();
        addPlayerSlots(playerInv);
        upgradeWindowOpen = false;
    }

    protected abstract void addMachineSlots();

    @Override
    public void setSynchronizer(ContainerSynchronizer synchronizer) {
        super.setSynchronizer(menuPlayer instanceof ServerPlayer player
                ? new FactorySynchronizer(player, synchronizer) : synchronizer);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack stored = slot.getItem();
            if (stored.getCount() > stored.getMaxStackSize()) {
                if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
                    ItemStack hotbar = player.getInventory().getItem(button);
                    if (slot.mayPickup(player) && (hotbar.isEmpty() || ItemStack.isSameItemSameTags(stored, hotbar))) {
                        int room = stored.getMaxStackSize() - hotbar.getCount();
                        if (room > 0) {
                            ItemStack taken = slot.remove(room);
                            if (hotbar.isEmpty()) {
                                player.getInventory().setItem(button, taken);
                            } else {
                                hotbar.grow(taken.getCount());
                            }
                            slot.onTake(player, taken);
                        }
                    }
                    return;
                }
                if (type == ClickType.PICKUP && !getCarried().isEmpty()
                        && !ItemStack.isSameItemSameTags(stored, getCarried())) {
                    return;
                }
            }
        }
        super.clicked(slotId, button, type, player);
    }

    protected void addEnergySlot() {
        addEnergySlot(ENERGY_SLOT_X, ENERGY_SLOT_Y);
    }

    protected void addEnergySlot(int x, int y) {
        energySlot = addSlot(new FilteredSlot(factory.getEnergyItem(), 0, x, y, factory::isValidEnergyItem));
    }

    public Slot getEnergySlot() {
        return energySlot;
    }

    /** Horizontal anchor shared by the energy slot and the client power bar. */
    public int energyBarX() {
        return ENERGY_SLOT_X;
    }

    protected void addVillagerSlot(int x, int y) {
        addSlot(new FilteredSlot(factory.getVillagerHandler(), 0, x, y, stack -> stack.getItem() instanceof VillagerItem));
    }

    protected void addItemGrid(ItemStackHandler handler, int startX, int startY, boolean output) {
        for (int i = 0; i < handler.getSlots(); i++) {
            int x = startX + (i % 3) * 18;
            int y = startY + (i / 3) * 18;
            addSlot(output ? new OutputSlot(handler, i, x, y) : new SlotItemHandler(handler, i, x, y));
        }
    }

    protected static int gridRows(int slots) {
        return slots <= 0 ? 0 : (slots + 2) / 3;
    }

    protected void addUpgradeSlots() {
        upgradeSlots.add(addSlot(new FilteredSlot(factory.getUpgradeInput(), 0, upgradeSlotX(), upgradeSlotInputY(),
                factory::isSupportedUpgrade, 64, () -> false)));
        upgradeSlots.add(addSlot(new OutputSlot(factory.getUpgradeOutput(), 0, upgradeSlotX(), upgradeSlotOutputY(),
                () -> false)));
    }

    /**
     * The upgrade window owns the visual placement of these menu slots. Most
     * factories keep them at their standard backing positions, while menus
     * with a denser machine layout can safely keep the backing slots outside
     * of the main click area.
     */
    protected int upgradeSlotX() {
        return UPGRADE_SLOT_X;
    }

    protected int upgradeSlotInputY() {
        return UPGRADE_SLOT_Y;
    }

    protected int upgradeSlotOutputY() {
        return UPGRADE_SLOT_OUTPUT_Y;
    }

    public List<Slot> getUpgradeSlots() {
        return upgradeSlots;
    }

    public boolean isUpgradeSlotActive() {
        return upgradeWindowOpen;
    }

    public void setUpgradeWindowOpen(boolean open) {
        upgradeWindowOpen = open;
    }

    protected void addPlayerSlots(Inventory playerInv) {
        int y = playerInventoryY();
        int xOffset = playerInventoryX();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, xOffset + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, xOffset + col * 18, y + 58));
        }
    }

    public abstract int playerInventoryY();

    /** Centers the standard 162px-wide player inventory in wider factory GUIs. */
    public int playerInventoryX() {
        return Math.max(8, (guiWidth() - 162) / 2);
    }

    public int guiWidth() {
        return 176;
    }

    public VillagerFactoryBlockEntity getFactory() {
        return factory;
    }

    private int syncedValue(int index) {
        if (menuPlayer.level().isClientSide) {
            return syncedData.get(index * 2) | (syncedData.get(index * 2 + 1) << 16);
        }
        return data.get(index);
    }

    public int getEnergy() {
        return syncedValue(0);
    }

    public int getMaxEnergy() {
        return Math.max(1, syncedValue(1));
    }

    public int getRedstoneOrdinal() {
        return syncedValue(2);
    }

    public boolean isAutoEject() {
        return syncedValue(3) != 0;
    }

    public boolean isAutoSort() {
        return syncedValue(23) != 0;
    }

    public boolean isTradeEnabled() {
        return syncedValue(24) != 0;
    }

    public boolean hasStrictInput() {
        return syncedValue(14) != 0;
    }

    public EnumColor getOutputColor() {
        return decodeColor(syncedValue(15));
    }

    public EnumColor getInputColor(RelativeSide side) {
        return decodeColor(syncedValue(16 + side.ordinal()));
    }

    public SecurityMode getSecurityMode() {
        int ordinal = syncedValue(22);
        return ordinal >= 0 && ordinal < SecurityMode.values().length
                ? SecurityMode.byIndexStatic(ordinal) : SecurityMode.PUBLIC;
    }

    private static EnumColor decodeColor(int encoded) {
        int ordinal = encoded - 1;
        return ordinal >= 0 && ordinal < EnumColor.values().length ? EnumColor.byIndexStatic(ordinal) : null;
    }

    public int getProcesses() {
        // ContainerData is populated after the client menu is constructed. Use
        // the block tier as a stable fallback so all process widgets/slots are
        // present on the first frame of a newly opened GUI.
        int synced = syncedValue(4);
        return Math.max(1, synced > 0 ? synced : factory.getTier().processes());
    }

    public int getTradeIndex() {
        return syncedValue(5);
    }

    public int getTradeCount() {
        return syncedValue(6);
    }

    public int getProgress() {
        return syncedValue(7);
    }

    public int getMaxProgress() {
        int synced = syncedValue(8);
        return Math.max(1, synced > 0 ? synced : factory.getMaxProgress());
    }

    public FactoryStatus getStatus() {
        FactoryStatus[] values = FactoryStatus.values();
        int ordinal = syncedValue(9);
        if (ordinal < 0 || ordinal >= values.length) {
            return FactoryStatus.IDLE;
        }
        return values[ordinal];
    }

    public int getEnergyUsage() {
        return Math.max(0, syncedValue(10));
    }

    public int getTradeUses() {
        return Math.max(0, syncedValue(11));
    }

    public int getTradeMaxUses() {
        return Math.max(0, syncedValue(12));
    }

    public int getTradeDuration() {
        return getMaxProgress();
    }

    public int getSlotTradeIndex(int slot) {
        return slotData(slot, 0);
    }

    public int getSlotProgress(int slot) {
        return slotData(slot, 1);
    }

    public boolean isSlotPaused(int slot) {
        return slotData(slot, 2) != 0;
    }

    public FactoryStatus getSlotStatus(int slot) {
        FactoryStatus[] values = FactoryStatus.values();
        int ordinal = slotData(slot, 3);
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : FactoryStatus.IDLE;
    }

    private int slotData(int slot, int field) {
        if (slot < 0 || slot >= getProcesses() || field < 0 || field > 3) {
            return 0;
        }
        return syncedValue(25 + slot * 4 + field);
    }

    public double getUpgradeProgress() {
        return Math.min(1.0, Math.max(0.0, syncedValue(13) / 20.0));
    }

    public Component getStatusText() {
        return getStatus().displayName();
    }

    public RedstoneMode getRedstoneMode() {
        RedstoneMode[] values = RedstoneMode.values();
        int ordinal = getRedstoneOrdinal();
        if (ordinal < 0 || ordinal >= values.length) {
            return RedstoneMode.IGNORED;
        }
        return values[ordinal];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!factory.canAccess(player)) {
            return false;
        }
        if (id == BUTTON_SECURITY_NEXT || id == BUTTON_SECURITY_PREV) {
            factory.cycleSecurity(player, id == BUTTON_SECURITY_PREV);
            return true;
        }
        if (id == BUTTON_REDSTONE) {
            factory.cycleRedstone();
            return true;
        }
        if (id == BUTTON_REDSTONE_PREV) {
            factory.cycleRedstonePrev();
            return true;
        }
        if (id == BUTTON_REDSTONE_IGNORED) {
            factory.setRedstoneMode(RedstoneMode.IGNORED);
            return true;
        }
        if (id == BUTTON_REDSTONE_HIGH) {
            factory.setRedstoneMode(RedstoneMode.HIGH);
            return true;
        }
        if (id == BUTTON_REDSTONE_LOW) {
            factory.setRedstoneMode(RedstoneMode.LOW);
            return true;
        }
        if (id == BUTTON_EJECT) {
            factory.toggleAutoEject();
            return true;
        }
        if (id == BUTTON_AUTO_SORT) {
            factory.toggleAutoSort();
            return true;
        }
        if (id == BUTTON_TRADE_PAUSE && factory instanceof com.dasien.mekv.blockentity.TraderFactoryBlockEntity trader) {
            trader.toggleTradeEnabled();
            return true;
        }
        if (factory instanceof com.dasien.mekv.blockentity.TraderFactoryBlockEntity trader) {
            if (id >= BUTTON_TRADE_SELECT_GLOBAL_START
                    && id < BUTTON_TRADE_SELECT_GLOBAL_START + TRADE_SELECTION_STRIDE) {
                trader.setGlobalTradeIndex(id - BUTTON_TRADE_SELECT_GLOBAL_START);
                return true;
            }
            if (id >= BUTTON_TRADE_SELECT_PROCESS_START) {
                int encoded = id - BUTTON_TRADE_SELECT_PROCESS_START;
                int process = encoded / TRADE_SELECTION_STRIDE;
                int tradeIndex = encoded % TRADE_SELECTION_STRIDE;
                if (process < trader.getProcessCount()) {
                    trader.setProcessTradeIndex(process, tradeIndex);
                    return true;
                }
            }
            if (id >= BUTTON_TRADE_SLOT_PAUSE_START
                    && id < BUTTON_TRADE_SLOT_PAUSE_START + trader.getProcessCount()) {
                trader.toggleSlotPaused(id - BUTTON_TRADE_SLOT_PAUSE_START);
                return true;
            }
        }
        if (id == BUTTON_STRICT_INPUT) {
            factory.toggleStrictInput();
            return true;
        }
        if (id == BUTTON_OUTPUT_COLOR_NEXT || id == BUTTON_OUTPUT_COLOR_PREV) {
            factory.cycleOutputColor(id == BUTTON_OUTPUT_COLOR_PREV);
            return true;
        }
        if (id == BUTTON_OUTPUT_COLOR_CLEAR) {
            factory.clearOutputColor();
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_NEXT_START && id < BUTTON_INPUT_COLOR_NEXT_START + RelativeSide.values().length) {
            factory.cycleInputColor(RelativeSide.values()[id - BUTTON_INPUT_COLOR_NEXT_START], false);
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_PREV_START && id < BUTTON_INPUT_COLOR_PREV_START + RelativeSide.values().length) {
            factory.cycleInputColor(RelativeSide.values()[id - BUTTON_INPUT_COLOR_PREV_START], true);
            return true;
        }
        if (id >= BUTTON_INPUT_COLOR_CLEAR_START && id < BUTTON_INPUT_COLOR_CLEAR_START + RelativeSide.values().length) {
            factory.clearInputColor(RelativeSide.values()[id - BUTTON_INPUT_COLOR_CLEAR_START]);
            return true;
        }
        if (id >= BUTTON_SIDE_START && id < BUTTON_SIDE_START + RelativeSide.values().length) {
            factory.cycleSide(RelativeSide.values()[id - BUTTON_SIDE_START]);
            return true;
        }
        if (id >= BUTTON_ENERGY_SIDE_START && id < BUTTON_ENERGY_SIDE_START + RelativeSide.values().length) {
            factory.cycleEnergySide(RelativeSide.values()[id - BUTTON_ENERGY_SIDE_START]);
            return true;
        }
        if (id >= BUTTON_SIDE_PREV_START && id < BUTTON_SIDE_PREV_START + RelativeSide.values().length) {
            factory.cycleSidePrev(RelativeSide.values()[id - BUTTON_SIDE_PREV_START]);
            return true;
        }
        if (id == BUTTON_CLEAR_ITEM_SIDES) {
            factory.clearItemSides();
            return true;
        }
        if (id == BUTTON_CLEAR_ENERGY_SIDES) {
            factory.clearEnergySides();
            return true;
        }
        if (id == BUTTON_UNINSTALL_INFINITE || id == BUTTON_UNINSTALL_INFINITE_ALL) {
            factory.uninstallUpgrade(player, null, true, id == BUTTON_UNINSTALL_INFINITE_ALL);
            return true;
        }
        if (id == BUTTON_UPGRADE_WINDOW_OPEN) {
            setUpgradeWindowOpen(true);
            return true;
        }
        if (id == BUTTON_UPGRADE_WINDOW_CLOSE) {
            setUpgradeWindowOpen(false);
            return true;
        }
        if (id >= BUTTON_UNINSTALL_ONE && id < BUTTON_UNINSTALL_ONE + 10) {
            factory.uninstallUpgrade(player, Upgrade.byIndexStatic(id - BUTTON_UNINSTALL_ONE), false, false);
            return true;
        }
        if (id >= BUTTON_UNINSTALL_ALL && id < BUTTON_UNINSTALL_ALL + 10) {
            factory.uninstallUpgrade(player, Upgrade.byIndexStatic(id - BUTTON_UNINSTALL_ALL), false, true);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockEntity be = factory;
        return be.getLevel() != null && be.getLevel().getBlockEntity(be.getBlockPos()) == be
                && factory.canAccess(player)
                && player.distanceToSqr(be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem() && slot.mayPickup(player)) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            int playerStart = slots.size() - 36;
            boolean moved;
            if (index >= playerStart) {
                int upgradeStart = playerStart - upgradeSlots.size();
                if (isUpgradeSlotActive() && factory.isSupportedUpgrade(stack)) {
                    moved = moveItemStackTo(stack, upgradeStart, upgradeStart + 1, false);
                } else if (factory.isValidEnergyItem(stack)) {
                    moved = moveItemStackTo(stack, energySlot.index, energySlot.index + 1, false);
                } else {
                    moved = moveItemStackTo(stack, 0, upgradeStart, false);
                }
            } else {
                moved = moveItemStackTo(stack, playerStart, slots.size(), true);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    protected static ContainerData energyData(VillagerFactoryBlockEntity factory) {
        return new ContainerData() {
            private final int[] clientValues = new int[25 + Math.max(0, factory.getProcessCount()) * 4];

            @Override
            public int get(int index) {
                if (factory.getLevel() != null && factory.getLevel().isClientSide) {
                    return index >= 0 && index < clientValues.length ? clientValues[index] : 0;
                }
                return switch (index) {
                    case 0 -> factory.getEnergy().getEnergyStored();
                    case 1 -> factory.getEnergy().getMaxEnergyStored();
                    case 2 -> factory.getRedstoneMode().ordinal();
                    case 3 -> factory.isAutoEject() ? 1 : 0;
                    case 4 -> factory.getTier().processes();
                    case 5 -> factory.getTradeIndex();
                    case 6 -> factory.getTradeCount();
                    case 7 -> factory.getProgress();
                    case 8 -> factory.getMaxProgress();
                    case 9 -> factory.getStatus().ordinal();
                    case 10 -> factory.getEnergyUsage();
                    case 11 -> factory.getTradeUses();
                    case 12 -> factory.getTradeMaxUses();
                    case 13 -> factory.getUpgradeTicks();
                    case 14 -> factory.hasStrictInput() ? 1 : 0;
                    case 15 -> factory.getOutputColor() == null ? 0 : factory.getOutputColor().ordinal() + 1;
                    case 16, 17, 18, 19, 20, 21 -> {
                        EnumColor color = factory.getInputColor(RelativeSide.values()[index - 16]);
                        yield color == null ? 0 : color.ordinal() + 1;
                    }
                    case 22 -> factory.getSecurityMode().ordinal();
                    case 23 -> factory.isAutoSort() ? 1 : 0;
                    case 24 -> factory instanceof com.dasien.mekv.blockentity.TraderFactoryBlockEntity trader
                            && trader.isTradeEnabled() ? 1 : 0;
                    default -> {
                        int slotData = index - 25;
                        if (slotData >= 0 && slotData < factory.getProcessCount() * 4) {
                            int slot = slotData / 4;
                            yield switch (slotData % 4) {
                                case 0 -> factory.getTradeIndex(slot);
                                case 1 -> factory.getProgress(slot);
                                case 2 -> factory.isSlotPaused(slot) ? 1 : 0;
                                case 3 -> factory.getProcessStatus(slot).ordinal();
                                default -> 0;
                            };
                        }
                        yield 0;
                    }
                };
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < clientValues.length) {
                    clientValues[index] = value;
                }
            }

            @Override
            public int getCount() {
                return clientValues.length;
            }
        };
    }

    protected static <T extends VillagerFactoryBlockEntity> T getFactory(Inventory inv, FriendlyByteBuf buf, Class<T> type) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        if (type.isInstance(be)) {
            return type.cast(be);
        }
        throw new IllegalStateException("Invalid factory block entity at menu open");
    }
}
