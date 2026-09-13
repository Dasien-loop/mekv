package com.dasien.mekv.blockentity;

import com.dasien.mekv.Config;
import com.dasien.mekv.inventory.FactoryStackHandler;
import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.energy.FactoryEnergyStorage;
import com.dasien.mekv.factory.FactoryStatus;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.RedstoneMode;
import com.dasien.mekv.factory.RelativeSide;
import com.dasien.mekv.factory.SideMode;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.inventory.FactoryItemHandler;
import com.dasien.mekv.inventory.SingleStackHandler;
import com.dasien.mekv.item.InfiniteTradeUpgradeItem;
import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.registry.ModBlocks;
import com.dasien.mekv.util.FactoryHelper;
import de.maxhenkel.easyvillagers.blocks.VillagerBlockBase;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.items.ModItems;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import de.maxhenkel.easyvillagers.datacomponents.VillagerData;
import mekanism.api.Upgrade;
import mekanism.api.IConfigCardAccess;
import mekanism.api.IConfigurable;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.security.ISecurityObject;
import mekanism.api.security.SecurityMode;
import mekanism.api.text.EnumColor;

import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.inventory.TransitRequest;

import mekanism.common.item.interfaces.IUpgradeItem;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import mekanism.common.lib.security.SecurityUtils;
import mekanism.common.util.UpgradeUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public abstract class VillagerFactoryBlockEntity extends BlockEntity
        implements MenuProvider, IConfigCardAccess, ISecurityObject, Nameable {
    private static final int UPGRADE_TICKS_REQUIRED = 20;

    protected ItemStack villager = ItemStack.EMPTY;
    protected EasyVillagerEntity villagerEntity;
    protected FactoryStatus status = FactoryStatus.IDLE;

    protected final ItemStackHandler inputItems;
    protected final ItemStackHandler outputItems;
    protected final ItemStackHandler energyItem;
    protected final ItemStackHandler upgradeInput;
    protected final ItemStackHandler upgradeOutput;
    protected final Map<Upgrade, Integer> installedUpgrades = new EnumMap<>(Upgrade.class);
    protected boolean infiniteTradeUpgrade;
    protected int upgradeTicks;
    protected final FactoryEnergyStorage energy;
    protected final IItemHandlerModifiable villagerHandler;
    private IItemHandlerModifiable automationInputItems;

    protected final Map<RelativeSide, SideMode> sideModes = new EnumMap<>(RelativeSide.class);
    protected final Map<RelativeSide, Boolean> energySides = new EnumMap<>(RelativeSide.class);
    protected final Map<RelativeSide, EnumColor> inputColors = new EnumMap<>(RelativeSide.class);
    protected RedstoneMode redstoneMode = RedstoneMode.IGNORED;
    protected boolean autoEject;
    protected boolean autoSort;
    protected boolean strictInput;
    @Nullable
    protected EnumColor outputColor;
    private boolean tierUpgradeInProgress;
    @Nullable
    private UUID ownerUUID;
    private String ownerName = "";
    private SecurityMode securityMode = SecurityMode.PUBLIC;
    @Nullable
    private Component customName;

    protected VillagerFactoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean hasInput) {
        super(type, pos, state);
        FactoryTier tier = getTier();
        inputItems = new FactoryStackHandler(hasInput ? tier.processes() : 0, tier.stackMultiplier()) {
            // ItemStackHandler stores the caller's reference when an empty
            // slot accepts a complete stack. Menus and automation are allowed
            // to mutate their input stack afterwards, which can otherwise
            // silently change (or empty) the factory slot. Keep owned copies
            // so distribution always works from stable slot contents.
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack.copy());
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Container clicks are predicted on the client. Do not mutate
                // the client-side handler: the server owns lane assignment
                // and auto-sort distribution, and will send the authoritative
                // contents back through the menu synchronizer.
                if (level != null && level.isClientSide && !simulate) {
                    return stack.copy();
                }
                return super.insertItem(slot, stack.copy(), simulate);
            }

            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged();
                onInputChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isValidInputItem(slot, stack);
            }
        };
        int outputSlotCount = getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || getFactoryType() == VillagerFactoryType.FARMER
                ? tier.processes() * 2 : tier.processes();
        outputItems = new FactoryStackHandler(outputSlotCount, tier.stackMultiplier()) {
            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                super.setStackInSlot(slot, stack.copy());
            }

            @Override
            protected void onContentsChanged(int slot) {
                onInventoryChanged();
            }
        };
        automationInputItems = inputItems;
        energyItem = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isValidEnergyItem(stack);
            }
        };
        upgradeInput = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return VillagerFactoryBlockEntity.this.isSupportedUpgrade(stack);
            }

        };
        upgradeOutput = new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        energy = new FactoryEnergyStorage(tier.energyCapacity(), tier.maxEnergyTransfer(), this::setChanged);
        villagerHandler = new SingleStackHandler(
                this::getVillagerStack,
                this::setVillager,
                stack -> stack.getItem() instanceof VillagerItem);
        applyDefaultSides();
        rebuildItemCaps();
    }

    /**
     * Validates items entering a processing input slot. Subclasses can add
     * recipe/trade-specific restrictions while retaining the common factory
     * exclusions for villagers and upgrade items.
     */
    protected boolean isValidInputItem(int slot, @NotNull ItemStack stack) {
        return !(stack.getItem() instanceof VillagerItem) && !isUpgradeItem(stack);
    }


    protected void onInventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    protected void onInputChanged() {
    }

    protected void applyDefaultSides() {
        for (RelativeSide side : RelativeSide.values()) {
            sideModes.put(side, SideMode.NONE);
            energySides.put(side, true);
        }
        if (hasAutomationInput()) {
            sideModes.put(RelativeSide.FRONT, SideMode.INPUT);
            sideModes.put(RelativeSide.BACK, SideMode.OUTPUT);
        } else {
            // Output-only factories expose every side for item extraction,
            // matching Mekanism's machine-side configuration defaults.
            for (RelativeSide side : RelativeSide.values()) {
                sideModes.put(side, SideMode.OUTPUT);
            }
        }
    }

    protected final void setAutomationInputItems(IItemHandlerModifiable handler) {
        automationInputItems = handler;
        applyDefaultSides();
        rebuildItemCaps();
    }

    private boolean hasAutomationInput() {
        return automationInputItems.getSlots() > 0;
    }

    public FactoryTier getTier() {
        if (getBlockState().getBlock() instanceof VillagerFactoryBlock block) {
            return block.getTier();
        }
        return FactoryTier.BASIC;
    }

    public VillagerFactoryType getFactoryType() {
        if (getBlockState().getBlock() instanceof VillagerFactoryBlock block) {
            return block.getFactoryType();
        }
        return VillagerFactoryType.TRADER;
    }

    public Direction getFacing() {
        if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    public SideMode getSideMode(RelativeSide side) {
        return sideModes.getOrDefault(side, SideMode.NONE);
    }

    public SideMode getSideMode(@Nullable Direction worldSide) {
        if (worldSide == null) {
        return hasAutomationInput() ? SideMode.INPUT_OUTPUT : SideMode.OUTPUT;
        }
        return getSideMode(RelativeSide.fromWorld(getFacing(), worldSide));
    }

    public void cycleSide(RelativeSide side) {
        boolean allowInput = hasAutomationInput();
        sideModes.put(side, getSideMode(side).next(allowInput));
        rebuildItemCaps();
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    public void cycleSidePrev(RelativeSide side) {
        boolean allowInput = hasAutomationInput();
        sideModes.put(side, getSideMode(side).prev(allowInput));
        rebuildItemCaps();
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    public void clearItemSides() {
        for (RelativeSide side : RelativeSide.values()) {
            sideModes.put(side, SideMode.NONE);
        }
        rebuildItemCaps();
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    public void clearEnergySides() {
        for (RelativeSide side : RelativeSide.values()) {
            energySides.put(side, false);
        }
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    public void uninstallUpgrade(net.minecraft.world.entity.player.Player player, @Nullable Upgrade type, boolean infinite, boolean all) {
        if (infinite) {
            uninstallInfiniteTradeUpgrade();
            return;
        }
        if (type == null || !supportsUpgrade(type)) {
            return;
        }
        int installed = countUpgrade(type);
        if (installed <= 0) {
            return;
        }
        int requested = all ? installed : 1;
        ItemStack result = UpgradeUtils.getStack(type, requested);
        ItemStack remaining = upgradeOutput.insertItem(0, result, true);
        int removable = requested - remaining.getCount();
        if (removable <= 0) {
            return;
        }
        installedUpgrades.put(type, installed - removable);
        if (installed == removable) {
            installedUpgrades.remove(type);
        }
        upgradeOutput.insertItem(0, UpgradeUtils.getStack(type, removable), false);
        onUpgradesChanged();
    }

    private void uninstallInfiniteTradeUpgrade() {
        if (!infiniteTradeUpgrade) {
            return;
        }
        ItemStack upgrade = new ItemStack(com.dasien.mekv.registry.ModItems.INFINITE_TRADE_UPGRADE.get());
        if (!upgradeOutput.insertItem(0, upgrade, true).isEmpty()) {
            return;
        }
        infiniteTradeUpgrade = false;
        upgradeOutput.insertItem(0, upgrade, false);
        onUpgradesChanged();
    }

    public boolean isEnergySide(RelativeSide side) {
        return energySides.getOrDefault(side, true);
    }

    public boolean isEnergySide(@Nullable Direction worldSide) {
        if (worldSide == null) {
            return true;
        }
        return isEnergySide(RelativeSide.fromWorld(getFacing(), worldSide));
    }

    public void cycleEnergySide(RelativeSide side) {
        energySides.put(side, !isEnergySide(side));
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    private void notifyCableNeighbors() {
        if (level != null && !level.isClientSide) {
            WorldUtils.notifyLoadedNeighborsOfTileChange(level, worldPosition);
        }
    }

    public void cycleRedstone() {
        setRedstoneMode(redstoneMode.next());
    }

    public void cycleRedstonePrev() {
        setRedstoneMode(redstoneMode.prev());
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode == null || mode == redstoneMode) {
            return;
        }
        redstoneMode = mode;
        setChanged();
        sync();
    }

    public void toggleAutoEject() {
        autoEject = !autoEject;
        setChanged();
        sync();
    }

    public void toggleAutoSort() {
        if (getFactoryType() != VillagerFactoryType.TRADER) {
            return;
        }
        autoSort = !autoSort;
        onAutoSortChanged();
        setChanged();
        sync();
    }

    protected void onAutoSortChanged() {
    }

    public boolean isAutoSort() {
        return autoSort;
    }

    public void toggleStrictInput() {
        strictInput = !strictInput;
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    public boolean hasStrictInput() {
        return strictInput;
    }

    @Nullable
    public EnumColor getInputColor(RelativeSide side) {
        return inputColors.get(side);
    }

    @Nullable
    public EnumColor getOutputColor() {
        return outputColor;
    }

    public void cycleInputColor(RelativeSide side, boolean reverse) {
        setInputColor(side, cycleColor(getInputColor(side), reverse));
    }

    public void clearInputColor(RelativeSide side) {
        setInputColor(side, null);
    }

    public void cycleOutputColor(boolean reverse) {
        outputColor = cycleColor(outputColor, reverse);
        setChanged();
        sync();
    }

    public void clearOutputColor() {
        if (outputColor != null) {
            outputColor = null;
            setChanged();
            sync();
        }
    }

    private void setInputColor(RelativeSide side, @Nullable EnumColor color) {
        if (color == null) {
            inputColors.remove(side);
        } else {
            inputColors.put(side, color);
        }
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    @Nullable
    private static EnumColor cycleColor(@Nullable EnumColor current, boolean reverse) {
        int colorCount = EnumColor.values().length;
        int encoded = current == null ? 0 : current.ordinal() + 1;
        encoded = Math.floorMod(encoded + (reverse ? -1 : 1), colorCount + 1);
        return encoded == 0 ? null : EnumColor.BY_ID.apply(encoded - 1);
    }

    public boolean canTransporterInsert(@Nullable EnumColor color, Direction transporterSide) {
        if (!strictInput) {
            return true;
        }
        RelativeSide side = RelativeSide.fromWorld(getFacing(), transporterSide.getOpposite());
        EnumColor expected = getInputColor(side);
        return expected == null || expected == color;
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public boolean isAutoEject() {
        return autoEject;
    }

    public boolean isTierUpgradeInProgress() {
        return tierUpgradeInProgress;
    }

    public boolean isInUse() {
        if (level == null) {
            return false;
        }
        return level.players().stream().anyMatch(player -> player.containerMenu instanceof FactoryMenu menu
                && menu.getFactory() == this);
    }

    public boolean upgradeTier(FactoryTier targetTier) {
        if (level == null || level.isClientSide || targetTier == null || targetTier != getTier().next() || isInUse()) {
            return false;
        }
        CompoundTag data = saveWithoutMetadata(level.registryAccess());
        resizeSerializedHandler(data, "Input", getFactoryType() == VillagerFactoryType.TRADER ? targetTier.processes() : 0);
        int outputSlots = getFactoryType() == VillagerFactoryType.IRON_GOLEM
                || getFactoryType() == VillagerFactoryType.FARMER
                ? targetTier.processes() * 2 : targetTier.processes();
        resizeSerializedHandler(data, "Output", outputSlots);
        BlockState replacement = ModBlocks.get(getFactoryType(), targetTier).get().defaultBlockState();
        if (replacement.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            replacement = replacement.setValue(BlockStateProperties.HORIZONTAL_FACING, getFacing());
        }
        tierUpgradeInProgress = true;
        if (!level.setBlock(worldPosition, replacement, 3)) {
            tierUpgradeInProgress = false;
            return false;
        }
        BlockEntity upgraded = level.getBlockEntity(worldPosition);
        if (!(upgraded instanceof VillagerFactoryBlockEntity factory)) {
            return false;
        }
        factory.loadAdditional(data, level.registryAccess());
        factory.setChanged();
        factory.sync();
        factory.notifyCableNeighbors();
        return true;
    }

    public ItemStack createSustainedStack() {
        ItemStack stack = new ItemStack(getBlockState().getBlock());
        net.minecraft.world.item.BlockItem.setBlockEntityData(stack, (net.minecraft.world.level.block.entity.BlockEntityType) getType(), saveWithoutMetadata(level.registryAccess()));
        return stack;
    }

    public void prepareForSustainedRemoval() {
        tierUpgradeInProgress = true;
    }

    public int getComparatorLevel() {
        int slots = inputItems.getSlots() + outputItems.getSlots();
        if (slots == 0) {
            return 0;
        }
        double fullness = 0;
        for (int slot = 0; slot < inputItems.getSlots(); slot++) {
            fullness += (double) inputItems.getStackInSlot(slot).getCount() / inputItems.getSlotLimit(slot);
        }
        for (int slot = 0; slot < outputItems.getSlots(); slot++) {
            fullness += (double) outputItems.getStackInSlot(slot).getCount() / outputItems.getSlotLimit(slot);
        }
        return fullness <= 0 ? 0 : 1 + (int) Math.floor(14 * fullness / slots);
    }

    private static void resizeSerializedHandler(CompoundTag data, String key, int slots) {
        if (data.contains(key, Tag.TAG_COMPOUND)) {
            data.getCompound(key).putInt("Size", slots);
        }
    }

    public ItemStackHandler getInputItems() {
        return inputItems;
    }

    public ItemStackHandler getOutputItems() {
        return outputItems;
    }

    public ItemStackHandler getEnergyItem() {
        return energyItem;
    }

    public boolean isValidEnergyItem(ItemStack stack) {
        return !stack.isEmpty() && net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM.getCapability(stack, null)
                != null && net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM.getCapability(stack, null).canExtract();
    }

    public ItemStackHandler getUpgradeInput() {
        return upgradeInput;
    }

    public ItemStackHandler getUpgradeOutput() {
        return upgradeOutput;
    }

    public IItemHandler getVillagerHandler() {
        return villagerHandler;
    }

    public FactoryEnergyStorage getEnergy() {
        return energy;
    }

    public FactoryStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return 0;
    }

    public int getMaxProgress() {
        return 1;
    }

    public int getTradeIndex() {
        return 0;
    }

    public int getTradeCount() {
        return 0;
    }

    public int getTradeUses() {
        return 0;
    }

    public int getTradeMaxUses() {
        return 0;
    }

    /**
     * Number of independent process state entries exposed to a menu. Output
     * factories use the legacy aggregate progress path; trader factories
     * override this with one entry per input/output pair.
     */
    public int getProcessCount() {
        return 0;
    }

    public int getProgress(int process) {
        return getProgress();
    }

    public int getTradeIndex(int process) {
        return getTradeIndex();
    }

    public boolean isSlotPaused(int process) {
        return false;
    }

    public FactoryStatus getProcessStatus(int process) {
        return getStatus();
    }

    public int getEnergyUsage() {
        return 0;
    }

    public boolean usesEnergy() {
        return true;
    }

    public Component getVillagerDisplayName() {
        EasyVillagerEntity entity = getVillagerEntity();
        return entity == null ? Component.translatable("gui.mekv.status.no_villager") : entity.getName();
    }

    public Component getVillagerProfessionName() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            return Component.translatable("gui.mekv.status.no_villager");
        }
        var profession = entity.getVillagerData().getProfession();
        var key = net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (key == null) {
            return Component.literal(profession.toString());
        }
        return Component.translatable("entity.minecraft.villager." + key.getPath());
    }

    public int getVillagerLevel() {
        EasyVillagerEntity entity = getVillagerEntity();
        return entity == null ? 0 : entity.getVillagerData().getLevel();
    }

    public boolean isVillagerBaby() {
        EasyVillagerEntity entity = getVillagerEntity();
        return entity != null && entity.isBaby();
    }

    public static boolean isUpgradeItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof InfiniteTradeUpgradeItem || stack.getItem() instanceof IUpgradeItem;
    }

    public boolean isSupportedUpgrade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof InfiniteTradeUpgradeItem) {
            return getFactoryType() == VillagerFactoryType.TRADER;
        }
        if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
            Upgrade type = upgradeItem.getUpgradeType(stack);
            return type != null && supportsUpgrade(type);
        }
        return false;
    }

    /**
     * Installs as many upgrades from a held stack as this factory can accept.
     * This mirrors Mekanism's Shift+right-click upgrade interaction while the
     * GUI upgrade slot continues to use the normal timed installation path.
     *
     * @return the number of items accepted from the stack
     */
    public int installUpgrade(ItemStack stack) {
        if (!isSupportedUpgrade(stack)) {
            return 0;
        }
        if (stack.getItem() instanceof InfiniteTradeUpgradeItem) {
            if (infiniteTradeUpgrade) {
                return 0;
            }
            infiniteTradeUpgrade = true;
            onUpgradesChanged();
            return 1;
        }
        if (!(stack.getItem() instanceof IUpgradeItem upgradeItem)) {
            return 0;
        }
        Upgrade type = upgradeItem.getUpgradeType(stack);
        if (type == null || !supportsUpgrade(type)) {
            return 0;
        }
        int installed = countUpgrade(type);
        int available = Math.max(0, type.getMax() - installed);
        int accepted = Math.min(stack.getCount(), available);
        if (accepted <= 0) {
            return 0;
        }
        installedUpgrades.put(type, installed + accepted);
        onUpgradesChanged();
        return accepted;
    }

    public boolean supportsUpgrade(Upgrade upgrade) {
        return upgrade != null && getFactoryType() != VillagerFactoryType.TRADER
                && (upgrade == Upgrade.SPEED || upgrade == Upgrade.ENERGY || upgrade == Upgrade.MUFFLING);
    }

    public boolean hasInfiniteTradeUpgrade() {
        return infiniteTradeUpgrade;
    }

    public int countUpgrade(Upgrade type) {
        return installedUpgrades.getOrDefault(type, 0);
    }

    public int getUpgradeTicks() {
        return upgradeTicks;
    }

    public int getParallel() {
        return Math.max(1, getTier().parallel());
    }

    public float speedMultiplier() {
        // Tier speed is a trader-only bonus. Farmer and iron golem factories
        // intentionally start at the Easy Villagers rate and are accelerated
        // exclusively by speed upgrades.
        float tierSpeed = getFactoryType() == VillagerFactoryType.TRADER ? getTier().speed() : 1.0f;
        return tierSpeed * speedUpgradeMultiplier();
    }

    public float speedUpgradeMultiplier() {
        return (float) Math.pow(upgradeMultiplier(), countUpgrade(Upgrade.SPEED) / 8.0);
    }

    public float energyUsageMultiplier() {
        return (float) Math.pow(upgradeMultiplier(), -countUpgrade(Upgrade.ENERGY) / 8.0);
    }

    public float energyOperationMultiplier() {
        return (float) Math.pow(upgradeMultiplier(),
                (countUpgrade(Upgrade.SPEED) - countUpgrade(Upgrade.ENERGY)) / 8.0);
    }

    public float energyCapacityMultiplier() {
        return (float) Math.pow(upgradeMultiplier(), countUpgrade(Upgrade.ENERGY) / 8.0);
    }

    public boolean isMuffled() {
        return countUpgrade(Upgrade.MUFFLING) > 0;
    }

    protected int scaledTicks(int base) {
        if (base <= 1) {
            return 1;
        }
        return scaleAmount(base, 1.0 / speedMultiplier());
    }

    protected int scaledEnergy(int base) {
        if (base <= 0) {
            return 0;
        }
        return scaleAmount(base, energyUsageMultiplier());
    }

    protected int scaledOperationEnergy(int base) {
        if (base <= 0) {
            return 0;
        }
        return scaleAmount(base, energyOperationMultiplier());
    }

    protected int parallelAmount(int base) {
        if (base <= 0) {
            return 0;
        }
        long amount = (long) base * getParallel();
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    private static int scaleAmount(int base, double multiplier) {
        long scaled = Math.round(base * multiplier);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, scaled));
    }

    protected static double upgradeMultiplier() {
        int value = MekanismConfig.general.maxUpgradeMultiplier.get();
        return value < 1 ? 10.0 : value;
    }

    protected void tickUpgrades() {
        ItemStack stack = upgradeInput.getStackInSlot(0);
        int installable = getInstallableUpgradeCount(stack);
        if (installable <= 0) {
            upgradeTicks = 0;
            return;
        }
        if (upgradeTicks < UPGRADE_TICKS_REQUIRED) {
            upgradeTicks++;
            return;
        }
        boolean infinite = stack.getItem() instanceof InfiniteTradeUpgradeItem;
        Upgrade type = stack.getItem() instanceof IUpgradeItem upgradeItem
                ? upgradeItem.getUpgradeType(stack)
                : null;
        upgradeInput.extractItem(0, installable, false);
        if (infinite) {
            infiniteTradeUpgrade = true;
        } else if (type != null) {
            installedUpgrades.put(type, countUpgrade(type) + installable);
        }
        upgradeTicks = 0;
        onUpgradesChanged();
    }

    private int getInstallableUpgradeCount(ItemStack stack) {
        if (stack.isEmpty() || !isSupportedUpgrade(stack)) {
            return 0;
        }
        if (stack.getItem() instanceof InfiniteTradeUpgradeItem) {
            return infiniteTradeUpgrade ? 0 : 1;
        }
        if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
            Upgrade type = upgradeItem.getUpgradeType(stack);
            if (type == null || !supportsUpgrade(type)) {
                return 0;
            }
            return Math.max(0, Math.min(stack.getCount(), type.getMax() - countUpgrade(type)));
        }
        return 0;
    }

    private void migrateLegacyUpgrades(CompoundTag legacyTag) {
        ItemStackHandler legacy = new ItemStackHandler(2);
        legacy.deserializeNBT(level.registryAccess(), legacyTag);
        for (int slot = 0; slot < legacy.getSlots(); slot++) {
            ItemStack stack = legacy.getStackInSlot(slot);
            if (!isSupportedUpgrade(stack)) {
                continue;
            }
            if (stack.getItem() instanceof InfiniteTradeUpgradeItem) {
                infiniteTradeUpgrade = true;
            } else if (stack.getItem() instanceof IUpgradeItem upgradeItem) {
                Upgrade type = upgradeItem.getUpgradeType(stack);
                if (type == null || !supportsUpgrade(type)) {
                    continue;
                }
                int count = Math.min(type.getMax(), countUpgrade(type) + stack.getCount());
                installedUpgrades.put(type, count);
            }
        }
    }

    protected void onUpgradesChanged() {
        refreshEnergyCapacity();
        setChanged();
        sync();
    }

    protected void refreshEnergyCapacity() {
        FactoryTier tier = getTier();
        int capacity = Math.max(1, (int) Math.round(tier.energyCapacity()
                * Config.factoryEnergyCapacityMultiplier * energyCapacityMultiplier()));
        int transfer = Math.max(0, (int) Math.round(Math.max(256, capacity / 40)
                * Config.factoryEnergyTransferMultiplier));
        energy.updateCapacity(capacity, transfer);
    }

    public boolean hasVillager() {
        return !villager.isEmpty();
    }

    public ItemStack getVillager() {
        // Easy Villagers 1.21.1 stores the complete villager state in its
        // data component. Keep the item representation current after server
        // side profession/trade mutations so client menus receive offers.
        if (level != null && !level.isClientSide && villagerEntity != null && !villager.isEmpty()) {
            VillagerData.applyToItem(villager, villagerEntity);
        }
        return villager;
    }

    public ItemStack getVillagerStack() {
        return getVillager();
    }

    @Nullable
    public EasyVillagerEntity getVillagerEntity() {
        if (villagerEntity == null && !villager.isEmpty() && level != null) {
            villagerEntity = VillagerData.createEasyVillager(villager, level);
            if (!level.isClientSide) {
                onAddVillager(villagerEntity);
            }
        }
        return villagerEntity;
    }

    public void setVillager(ItemStack stack) {
        removeTradingPlayer();
        villager = stack.copy();
        if (stack.isEmpty()) {
            villagerEntity = null;
        } else if (level != null) {
            villagerEntity = VillagerData.createEasyVillager(stack, level);
            if (villagerEntity != null && !level.isClientSide) {
                onAddVillager(villagerEntity);
                VillagerData.applyToItem(villager, villagerEntity);
            }
        } else {
            villagerEntity = null;
        }
        setChanged();
        sync();
    }

    public ItemStack removeVillager() {
        ItemStack stack = getVillager();
        setVillager(ItemStack.EMPTY);
        return stack;
    }

    protected void onAddVillager(EasyVillagerEntity entity) {
    }

    protected void removeTradingPlayer() {
        if (villagerEntity != null) {
            villagerEntity.setTradingPlayer(null);
        }
    }

    protected boolean advanceAge() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            return false;
        }
        int before = entity.getAge();
        int after = before + 1;
        entity.setAge(after);
        return before < 0 && after >= 0;
    }

    protected boolean canRun() {
        return level != null && redstoneMode.allows(level.hasNeighborSignal(worldPosition));
    }

    protected boolean consumeEnergy(int amount) {
        if (!usesEnergy() || amount <= 0) {
            return true;
        }
        return energy.consume(amount);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VillagerFactoryBlockEntity be) {
        be.tickServer();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VillagerFactoryBlockEntity be) {
        be.tickClient();
    }

    protected void tickServer() {
        chargeFromEnergyItem();
        tickUpgrades();
        if (!canRun()) {
            status = FactoryStatus.NO_REDSTONE;
            setProcessStatus(status);
            updateActiveState();
            return;
        }
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            status = FactoryStatus.NO_VILLAGER;
        } else {
            // Ambient sounds are intentionally throttled; factories tick every
            // game tick and should not generate a continuous sound stream.
            if (level.getGameTime() % 200 == 0) {
                playFactorySound(SoundEvents.VILLAGER_AMBIENT, true);
            }
            if (advanceAge()) {
                getVillager();
                sync();
            } else if (level.getGameTime() % 20 == 0) {
                getVillager();
            }
        }
        work();
        if (autoEject && level.getGameTime() % 10 == 0) {
            ejectOutputs();
        }
        updateActiveState();
    }

    private void updateActiveState() {
        if (level == null || level.isClientSide || !getBlockState().hasProperty(VillagerFactoryBlock.ACTIVE)) {
            return;
        }
        boolean active = status == FactoryStatus.WORKING;
        if (getBlockState().getValue(VillagerFactoryBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, getBlockState().setValue(VillagerFactoryBlock.ACTIVE, active), 3);
        }
    }

    private void chargeFromEnergyItem() {
        ItemStack stack = energyItem.getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        net.neoforged.neoforge.energy.IEnergyStorage source = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM.getCapability(stack, null);
        if (source != null) {
            int accepted = energy.receiveEnergy(Integer.MAX_VALUE, true);
            if (accepted <= 0) {
                return;
            }
            int available = source.extractEnergy(accepted, true);
            if (available <= 0) {
                return;
            }
            int extracted = source.extractEnergy(available, false);
            if (extracted > 0) {
                energy.receiveEnergy(extracted, false);
                energyItem.setStackInSlot(0, stack);
            }
        }
    }

    protected void playFactorySound(SoundEvent sound, boolean random) {
        if (isMuffled() || level == null) {
            return;
        }
        if (random) {
            VillagerBlockBase.playRandomVillagerSound(level, worldPosition, sound);
        } else {
            VillagerBlockBase.playVillagerSound(level, worldPosition, sound);
        }
    }

    protected void tickClient() {
    }

    protected abstract void work();

    /** Updates per-process status for machines that expose independent work lanes. */
    protected void setProcessStatus(FactoryStatus processStatus) {
    }

    protected void ejectOutputs() {
        if (level == null) {
            return;
        }
        for (RelativeSide side : RelativeSide.values()) {
            if (!getSideMode(side).itemOutput()) {
                continue;
            }
            Direction world = side.toWorld(getFacing());
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(world));
            if (neighbor == null) {
                continue;
            }
            if (neighbor instanceof TileEntityLogisticalTransporterBase transporter) {
                ejectToTransporter(transporter);
                continue;
            }
            net.neoforged.neoforge.items.IItemHandler target = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK.getCapability(level, neighbor.getBlockPos(), neighbor.getBlockState(), neighbor, world.getOpposite());
            if (target != null) {
                for (int slot = 0; slot < outputItems.getSlots(); slot++) {
                    ItemStack stack = outputItems.getStackInSlot(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    ItemStack remaining = FactoryHelper.insertIntoNeighbor(target, stack.copy());
                    outputItems.setStackInSlot(slot, remaining);
                }
            }
        }
    }

    private void ejectToTransporter(TileEntityLogisticalTransporterBase transporter) {
        for (int slot = 0; slot < outputItems.getSlots(); slot++) {
            ItemStack stack = outputItems.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            TransitRequest request = TransitRequest.simple(stack.copy());
            TransitRequest.TransitResponse response = transporter.getTransmitter()
                    .insert(this, worldPosition, request, outputColor, true, 0);
            int sent = response.getSendingAmount();
            if (sent > 0) {
                outputItems.extractItem(slot, sent, false);
            }
        }
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        if (hasVillager()) {
            FactoryHelper.dropHandler(level, worldPosition, new ItemStackHandler(1) {{
                setStackInSlot(0, removeVillager());
            }});
        }
        FactoryHelper.dropHandler(level, worldPosition, inputItems);
        FactoryHelper.dropHandler(level, worldPosition, outputItems);
        FactoryHelper.dropHandler(level, worldPosition, energyItem);
        FactoryHelper.dropHandler(level, worldPosition, upgradeInput);
        FactoryHelper.dropHandler(level, worldPosition, upgradeOutput);
        for (Map.Entry<Upgrade, Integer> entry : installedUpgrades.entrySet()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    UpgradeUtils.getStack(entry.getKey(), entry.getValue()));
        }
        if (infiniteTradeUpgrade) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                    new ItemStack(com.dasien.mekv.registry.ModItems.INFINITE_TRADE_UPGRADE.get()));
        }
    }

    protected void rebuildItemCaps() {
        // NeoForge capabilities are registered through RegisterCapabilitiesEvent;
        // inventory handlers remain directly accessible to menus and automation.
    }

    /** NeoForge capability provider accessors. */
    public IItemHandler getItemCapability(@Nullable Direction side) {
        if (side == null) {
            return new FactoryItemHandler(automationInputItems, outputItems, true, true);
        }
        return new FactoryItemHandler(automationInputItems, outputItems,
                () -> getSideMode(side).itemInput(), () -> getSideMode(side).itemOutput());
    }

    public IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return isEnergySide(side) ? energy : null;
    }

    private class FactoryConfigurable implements IConfigurable {
        private final Direction worldSide;

        private FactoryConfigurable(Direction worldSide) {
            this.worldSide = worldSide;
        }

        @Override
        public InteractionResult onSneakRightClick(Player player) {
            return configure(player, true);
        }

        @Override
        public InteractionResult onRightClick(Player player) {
            return configure(player, false);
        }

        private InteractionResult configure(Player player, boolean reverse) {
            if (!canAccess(player) || !(player.getMainHandItem().getItem() instanceof ItemConfigurator configurator)) {
                return InteractionResult.FAIL;
            }
            ItemConfigurator.ConfiguratorMode mode = configurator.getMode(player.getMainHandItem());
            RelativeSide side = RelativeSide.fromWorld(getFacing(), worldSide);
            if (mode == ItemConfigurator.ConfiguratorMode.CONFIGURATE_ITEMS) {
                if (reverse) {
                    cycleSidePrev(side);
                } else {
                    cycleSide(side);
                }
                return InteractionResult.SUCCESS;
            }
            if (mode == ItemConfigurator.ConfiguratorMode.CONFIGURATE_ENERGY) {
                cycleEnergySide(side);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        notifyCableNeighbors();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeTradingPlayer();
    }

    public void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            tag.put("Villager", getVillager().save(provider));
        }
        tag.put("Input", inputItems.serializeNBT(provider));
        tag.put("Output", outputItems.serializeNBT(provider));
        tag.put("EnergyItem", energyItem.serializeNBT(provider));
        tag.put("UpgradeInput", upgradeInput.serializeNBT(provider));
        tag.put("UpgradeOutput", upgradeOutput.serializeNBT(provider));
        CompoundTag installedTag = new CompoundTag();
        Upgrade.saveMap(installedUpgrades, installedTag);
        tag.put("InstalledUpgrades", installedTag);
        tag.putBoolean("InfiniteTradeUpgrade", infiniteTradeUpgrade);
        tag.putInt("UpgradeTicks", upgradeTicks);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putString("Redstone", redstoneMode.getSerializedName());
        tag.putBoolean("AutoEject", autoEject);
        tag.putBoolean("AutoSort", autoSort);
        tag.putBoolean("StrictInput", strictInput);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
        if (!ownerName.isEmpty()) {
            tag.putString("OwnerName", ownerName);
        }
        tag.putString("SecurityMode", securityMode.name());
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName, provider));
        }
        if (outputColor != null) {
            tag.putInt("OutputColor", outputColor.ordinal());
        }
        ListTag sides = new ListTag();
        for (RelativeSide side : RelativeSide.values()) {
            CompoundTag sideTag = new CompoundTag();
            sideTag.putString("Side", side.getSerializedName());
            sideTag.putString("Mode", getSideMode(side).getSerializedName());
            sideTag.putBoolean("Energy", isEnergySide(side));
            EnumColor inputColor = getInputColor(side);
            if (inputColor != null) {
                sideTag.putInt("InputColor", inputColor.ordinal());
            }
            sides.add(sideTag);
        }
        tag.put("Sides", sides);
        saveFactory(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("Villager")) {
            ItemStack loadedVillager = ItemStack.parse(provider, tag.getCompound("Villager")).orElse(ItemStack.EMPTY);
            boolean changed = villager.isEmpty()
                    || !ItemStack.matches(villager, loadedVillager)
                    || villager.getCount() != loadedVillager.getCount();
            villager = loadedVillager;
            villagerEntity = null;
            // Update the client-side render entity in place. Recreating it for every
            // progress packet resets animations and causes visible flicker.
            if (villagerEntity != null && changed) {
                if (villager.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                    villagerEntity.setCustomName(villager.getHoverName());
                }
            }
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }
        if (tag.contains("Input")) {
            inputItems.deserializeNBT(provider, withHandlerSize(tag.getCompound("Input"), inputItems.getSlots()));
        }
        if (tag.contains("Output")) {
            outputItems.deserializeNBT(provider, withHandlerSize(tag.getCompound("Output"), outputItems.getSlots()));
        }
        if (tag.contains("EnergyItem")) {
            energyItem.deserializeNBT(provider, tag.getCompound("EnergyItem"));
        }
        upgradeInput.setStackInSlot(0, ItemStack.EMPTY);
        upgradeOutput.setStackInSlot(0, ItemStack.EMPTY);
        installedUpgrades.clear();
        infiniteTradeUpgrade = false;
        upgradeTicks = 0;
        if (tag.contains("UpgradeInput")) {
            upgradeInput.deserializeNBT(provider, tag.getCompound("UpgradeInput"));
        }
        if (tag.contains("UpgradeOutput")) {
            upgradeOutput.deserializeNBT(provider, tag.getCompound("UpgradeOutput"));
        }
        if (tag.contains("InstalledUpgrades")) {
            Map<Upgrade, Integer> loaded = Upgrade.buildMap(tag.getCompound("InstalledUpgrades"));
            if (loaded != null) {
                loaded.forEach((type, count) -> {
                    if (supportsUpgrade(type) && count > 0) {
                        installedUpgrades.put(type, Math.min(count, type.getMax()));
                    }
                });
            }
            infiniteTradeUpgrade = getFactoryType() == VillagerFactoryType.TRADER
                    && tag.getBoolean("InfiniteTradeUpgrade");
            upgradeTicks = Math.max(0, Math.min(UPGRADE_TICKS_REQUIRED, tag.getInt("UpgradeTicks")));
        } else if (tag.contains("Upgrades")) {
            migrateLegacyUpgrades(tag.getCompound("Upgrades"));
        }
        refreshEnergyCapacity();
        energy.setEnergy(tag.getInt("Energy"));
        redstoneMode = parse(RedstoneMode.class, tag.getString("Redstone"), RedstoneMode.IGNORED);
        autoEject = tag.getBoolean("AutoEject");
        autoSort = getFactoryType() == VillagerFactoryType.TRADER && tag.getBoolean("AutoSort");
        strictInput = tag.getBoolean("StrictInput");
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        try {
            securityMode = SecurityMode.valueOf(tag.getString("SecurityMode"));
        } catch (IllegalArgumentException ignored) {
            securityMode = SecurityMode.PUBLIC;
        }
        customName = tag.contains("CustomName", Tag.TAG_STRING)
                ? Component.Serializer.fromJson(tag.getString("CustomName"), provider) : null;
        outputColor = readColor(tag, "OutputColor");
        inputColors.clear();
        if (tag.contains("Sides", Tag.TAG_LIST)) {
            ListTag sides = tag.getList("Sides", Tag.TAG_COMPOUND);
            for (int i = 0; i < sides.size(); i++) {
                CompoundTag sideTag = sides.getCompound(i);
                RelativeSide side = parse(RelativeSide.class, sideTag.getString("Side"), null);
                SideMode mode = parse(SideMode.class, sideTag.getString("Mode"), SideMode.NONE);
                if (side == null) {
                    continue;
                }
                if (mode == SideMode.ENERGY) {
                    sideModes.put(side, SideMode.NONE);
                    energySides.put(side, true);
                } else {
                    sideModes.put(side, mode);
                    energySides.put(side, sideTag.contains("Energy") ? sideTag.getBoolean("Energy") : true);
                }
                EnumColor inputColor = readColor(sideTag, "InputColor");
                if (inputColor != null) {
                    inputColors.put(side, inputColor);
                }
            }
        }
        loadFactory(tag);
        onAutoSortChanged();
        rebuildItemCaps();
    }

    @Nullable
    private static EnumColor readColor(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_INT)) {
            return null;
        }
        int ordinal = tag.getInt(key);
        return ordinal >= 0 && ordinal < EnumColor.values().length ? EnumColor.BY_ID.apply(ordinal) : null;
    }

    protected void saveFactory(CompoundTag tag) {
    }

    protected void loadFactory(CompoundTag tag) {
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, level.registryAccess());
        }
    }

    private static CompoundTag withHandlerSize(CompoundTag source, int size) {
        CompoundTag resized = source.copy();
        resized.putInt("Size", size);
        return resized;
    }

    @Override
    public Component getDisplayName() {
        return customName == null ? Component.translatable(getBlockState().getBlock().getDescriptionId()) : customName;
    }

    @Override
    public Component getName() {
        return getDisplayName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable Component customName) {
        this.customName = customName;
        setChanged();
        sync();
    }

    public void setOwner(Player player) {
        if (ownerUUID == null) {
            ownerUUID = player.getUUID();
            ownerName = player.getGameProfile().getName();
            setChanged();
            sync();
        }
    }

    public boolean canAccess(Player player) {
        return SecurityUtils.get().canAccessObject(player, this);
    }

    public void cycleSecurity(Player player, boolean reverse) {
        if (!ownerMatches(player)) {
            return;
        }
        SecurityMode[] modes = SecurityMode.values();
        int index = Math.floorMod(securityMode.ordinal() + (reverse ? -1 : 1), modes.length);
        setSecurityMode(modes[index]);
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public void setOwnerUUID(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
        setChanged();
        sync();
    }

    @Override
    public SecurityMode getSecurityMode() {
        return securityMode;
    }

    @Override
    public void setSecurityMode(SecurityMode mode) {
        if (mode != null && mode != securityMode) {
            SecurityMode old = securityMode;
            securityMode = mode;
            onSecurityChanged(old, mode);
            setChanged();
            sync();
        }
    }

    @Override
    public String getConfigCardName() {
        return getBlockState().getBlock().getDescriptionId();
    }

    @Override
    public net.minecraft.world.level.block.Block getConfigurationDataType() {
        return getBlockState().getBlock();
    }

    @Override
    public CompoundTag getConfigurationData(net.minecraft.core.HolderLookup.Provider provider, Player player) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Redstone", redstoneMode.getSerializedName());
        tag.putBoolean("AutoEject", autoEject);
        tag.putBoolean("AutoSort", autoSort);
        tag.putBoolean("StrictInput", strictInput);
        if (outputColor != null) {
            tag.putInt("OutputColor", outputColor.ordinal());
        }
        ListTag sides = new ListTag();
        for (RelativeSide side : RelativeSide.values()) {
            CompoundTag sideTag = new CompoundTag();
            sideTag.putString("Side", side.getSerializedName());
            sideTag.putString("Mode", getSideMode(side).getSerializedName());
            sideTag.putBoolean("Energy", isEnergySide(side));
            EnumColor color = getInputColor(side);
            if (color != null) {
                sideTag.putInt("InputColor", color.ordinal());
            }
            sides.add(sideTag);
        }
        tag.put("Sides", sides);
        return tag;
    }

    @Override
    public void setConfigurationData(net.minecraft.core.HolderLookup.Provider provider, Player player, CompoundTag tag) {
        redstoneMode = parse(RedstoneMode.class, tag.getString("Redstone"), RedstoneMode.IGNORED);
        autoEject = tag.getBoolean("AutoEject");
        autoSort = getFactoryType() == VillagerFactoryType.TRADER && tag.getBoolean("AutoSort");
        strictInput = tag.getBoolean("StrictInput");
        outputColor = readColor(tag, "OutputColor");
        inputColors.clear();
        if (tag.contains("Sides", Tag.TAG_LIST)) {
            for (Tag entry : tag.getList("Sides", Tag.TAG_COMPOUND)) {
                CompoundTag sideTag = (CompoundTag) entry;
                RelativeSide side = parse(RelativeSide.class, sideTag.getString("Side"), null);
                if (side == null) {
                    continue;
                }
                SideMode mode = parse(SideMode.class, sideTag.getString("Mode"), SideMode.NONE);
                if (!hasAutomationInput() && mode.itemInput()) {
                    mode = mode == SideMode.INPUT_OUTPUT ? SideMode.OUTPUT : SideMode.NONE;
                }
                sideModes.put(side, mode == SideMode.ENERGY ? SideMode.NONE : mode);
                energySides.put(side, sideTag.getBoolean("Energy"));
                EnumColor color = readColor(sideTag, "InputColor");
                if (color != null) {
                    inputColors.put(side, color);
                }
            }
        }
    }

    @Override
    public void configurationDataSet() {
        onAutoSortChanged();
        rebuildItemCaps();
        setChanged();
        sync();
        notifyCableNeighbors();
    }

    protected static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
        for (E value : type.getEnumConstants()) {
            if (value instanceof net.minecraft.util.StringRepresentable representable
                    && representable.getSerializedName().equals(name)) {
                return value;
            }
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }
}















