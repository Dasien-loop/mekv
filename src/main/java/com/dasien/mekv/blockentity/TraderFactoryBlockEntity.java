package com.dasien.mekv.blockentity;

import com.dasien.mekv.Config;
import com.dasien.mekv.factory.FactoryStatus;
import com.dasien.mekv.inventory.SingleStackHandler;
import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.menu.TraderFactoryMenu;
import com.dasien.mekv.registry.ModBlockEntities;
import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import mekanism.common.lib.inventory.HashedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class TraderFactoryBlockEntity extends VillagerFactoryBlockEntity {
    private static final int MIN_TRADE_DURATION_TICKS = 10;
    private static final int MAX_TRADE_DURATION_TICKS = 20;
    private Block workstation = Blocks.AIR;
    /** Global trade selection used by every process without an override. */
    private int tradeIndex;
    private boolean tradeEnabled = true;
    /** The selected offer for each independent processing lane. */
    private int[] processTradeIndices;
    private int[] processProgress;
    private boolean[] processPaused;
    private FactoryStatus[] processStatuses;
    /**
     * Input reserved when a lane starts. MerchantOffer prices are demand
     * based, so getCostA() may change while a lane is running. Keeping the
     * original requirement prevents a running lane from being invalidated by
     * another completed trade.
     */
    private ItemStack[] processReservedInputs;
    /** Offer index used when a lane started its current process. */
    private int[] processReservedTradeIndices;
    private long nextRestock;
    private long lastRestockGameTime;
    private boolean sortingNeeded = true;
    private boolean sortingInputs;
    /** Prevents intermediate slot callbacks while a distribution is committed. */
    private boolean batchingInventoryUpdate;
    private final IItemHandler workstationHandler;

    public TraderFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADER_FACTORY.get(), pos, state, true);
        initializeProcessState(getTier().processes());
        workstationHandler = new SingleStackHandler(
                this::getWorkstationStack,
                this::setWorkstationFromStack,
                stack -> stack.getItem() instanceof BlockItem blockItem && isValidWorkstation(blockItem.getBlock()));
    }

    public IItemHandler getWorkstationHandler() {
        return workstationHandler;
    }
    @Override
    protected boolean isValidInputItem(int slot, ItemStack stack) {
        ensureProcessState();
        if (!super.isValidInputItem(slot, stack) || stack.isEmpty() || !isValidProcess(slot)) {
            return false;
        }
        // Each lane owns one selected offer. Keep insertion lane-specific so
        // automation cannot place an item into a different trade's slot and
        // leave it permanently unprocessable.
        // Client-side container prediction must never query Villager#getOffers:
        // Minecraft 1.21.1 intentionally throws when offers are not locally
        // loaded. The authoritative server tick performs the lane-specific
        // price check before consuming anything.
        if (level != null && level.isClientSide) {
            return true;
        }
        return requiredInputAmount(slot, stack) > 0;
    }

    public ItemStack getWorkstationStack() {
        return hasWorkstation() ? new ItemStack(workstation) : ItemStack.EMPTY;
    }

    public void setWorkstationFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            setWorkstation(Blocks.AIR);
            return;
        }
        if (stack.getItem() instanceof BlockItem blockItem && isValidWorkstation(blockItem.getBlock())) {
            setWorkstation(blockItem.getBlock());
        }
    }

    private void initializeProcessState(int count) {
        int size = Math.max(0, count);
        processTradeIndices = new int[size];
        Arrays.fill(processTradeIndices, 0);
        processProgress = new int[size];
        processPaused = new boolean[size];
        processStatuses = new FactoryStatus[size];
        processReservedInputs = new ItemStack[size];
        Arrays.fill(processReservedInputs, ItemStack.EMPTY);
        processReservedTradeIndices = new int[size];
        Arrays.fill(processReservedTradeIndices, -1);
        Arrays.fill(processStatuses, FactoryStatus.IDLE);
    }

    private void ensureProcessState() {
        int size = Math.max(0, getTier().processes());
        if (processTradeIndices != null && processProgress != null && processPaused != null && processStatuses != null
                && processReservedInputs != null && processReservedTradeIndices != null
                && processTradeIndices.length == size
                && processProgress.length == size && processPaused.length == size
                && processStatuses.length == size && processReservedInputs.length == size
                && processReservedTradeIndices.length == size) {
            return;
        }
        int[] oldIndices = processTradeIndices;
        int[] oldProgress = processProgress;
        boolean[] oldPaused = processPaused;
        FactoryStatus[] oldStatuses = processStatuses;
        ItemStack[] oldReservedInputs = processReservedInputs;
        int[] oldReservedTradeIndices = processReservedTradeIndices;
        initializeProcessState(size);
        if (oldIndices != null && oldProgress != null && oldPaused != null && oldStatuses != null) {
            System.arraycopy(oldIndices, 0, processTradeIndices, 0, Math.min(oldIndices.length, size));
            System.arraycopy(oldProgress, 0, processProgress, 0, Math.min(oldProgress.length, size));
            System.arraycopy(oldPaused, 0, processPaused, 0, Math.min(oldPaused.length, size));
            System.arraycopy(oldStatuses, 0, processStatuses, 0, Math.min(oldStatuses.length, size));
            if (oldReservedInputs != null) {
                for (int i = 0; i < Math.min(oldReservedInputs.length, size); i++) {
                    processReservedInputs[i] = oldReservedInputs[i] == null
                            ? ItemStack.EMPTY : oldReservedInputs[i].copy();
                }
            }
            if (oldReservedTradeIndices != null) {
                System.arraycopy(oldReservedTradeIndices, 0, processReservedTradeIndices, 0,
                        Math.min(oldReservedTradeIndices.length, size));
            }
        }
    }

    public boolean hasWorkstation() {
        return workstation != Blocks.AIR;
    }

    public Block getWorkstation() {
        return workstation;
    }

    public boolean isValidWorkstation(Block block) {
        Optional<net.minecraft.core.Holder<PoiType>> poi = PoiTypes.forState(block.defaultBlockState());
        if (poi.isEmpty()) {
            return false;
        }
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            if (profession != VillagerProfession.NONE && profession.heldJobSite().test(poi.get())) {
                return true;
            }
        }
        return false;
    }

    public void setWorkstation(Block block) {
        workstation = block;
        resetAllProcesses();
        if (hasVillager()) {
            fixProfession();
        }
        updateTradeInv();
        setChanged();
        sync();
    }

    public Block removeWorkstation() {
        Block previous = workstation;
        setWorkstation(Blocks.AIR);
        return previous;
    }

    public VillagerProfession getWorkstationProfession() {
        Optional<net.minecraft.core.Holder<PoiType>> poi = PoiTypes.forState(workstation.defaultBlockState());
        if (poi.isEmpty()) {
            return VillagerProfession.NONE;
        }
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            if (profession.heldJobSite().test(poi.get())) {
                return profession;
            }
        }
        return VillagerProfession.NONE;
    }

    @Override
    protected void onAddVillager(EasyVillagerEntity entity) {
        resetAllProcesses();
        if (hasWorkstation()) {
            fixProfession();
        }
        updateTradeInv();
    }

    private void fixProfession() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null || entity.getVillagerXp() > 0 || entity.getVillagerData().getProfession().equals(VillagerProfession.NITWIT)) {
            return;
        }
        VillagerProfession profession = getWorkstationProfession();
        if (profession == VillagerProfession.NONE) {
            return;
        }
        if (entity.getVillagerData().getProfession().equals(profession)) {
            return;
        }
        entity.setVillagerData(entity.getVillagerData().setProfession(profession).setLevel(1));
        entity.overrideOffers(new MerchantOffers());
        // Easy Villagers 1.21.1 follows the vanilla 1.21 offer lifecycle.
        // Recalculate through the entity instead of manually appending the
        // legacy trade table; this also refreshes demand and synced offers.
        entity.recalculateOffers();
    }

    public void nextTrade() {
        int count = getTradeCount();
        if (count > 0) {
            setGlobalTradeIndex(Math.floorMod(tradeIndex + 1, count));
        }
    }

    public void prevTrade() {
        int count = getTradeCount();
        if (count > 0) {
            setGlobalTradeIndex(Math.floorMod(tradeIndex - 1, count));
        }
    }

    public void setTradeIndex(int index) {
        int count = getTradeCount();
        if (count <= 0) {
            return;
        }
        setGlobalTradeIndex(Math.floorMod(index, count));
    }

    /** Applies a validated offer index to the global selection and every lane. */
    public void setGlobalTradeIndex(int index) {
        int count = getTradeCount();
        if (index < 0 || index >= count) {
            return;
        }
        tradeIndex = index;
        updateTradeInv();
        ensureProcessState();
        for (int process = 0; process < processTradeIndices.length; process++) {
            // A lane with an input stack or an active reservation owns its
            // current offer until the operation is finished. Changing its
            // selection here would make the visible input incompatible with
            // the new trade.
            if (processProgress[process] > 0
                    || !inputItems.getStackInSlot(process).isEmpty()) {
                continue;
            }
            processTradeIndices[process] = tradeIndex;
            resetProcess(process);
        }
        sortingNeeded = true;
        setChanged();
        sync();
    }

    /** Applies one exact offer to one processing lane. */
    public void setProcessTradeIndex(int process, int index) {
        ensureProcessState();
        int count = getTradeCount();
        if (!isValidProcess(process) || index < 0 || index >= count) {
            return;
        }
        if (processProgress[process] > 0
                || !inputItems.getStackInSlot(process).isEmpty()) {
            return;
        }
        processTradeIndices[process] = index;
        resetProcess(process);
        sortingNeeded = true;
        setChanged();
        sync();
    }

    public void toggleSlotPaused(int process) {
        ensureProcessState();
        if (!isValidProcess(process)) {
            return;
        }
        processPaused[process] = !processPaused[process];
        processStatuses[process] = !tradeEnabled || processPaused[process]
                ? FactoryStatus.PAUSED : FactoryStatus.IDLE;
        sortingNeeded = true;
        setChanged();
        sync();
    }

    @Override
    public int getProcessCount() {
        ensureProcessState();
        return processProgress.length;
    }

    @Override
    public int getTradeIndex(int process) {
        ensureProcessState();
        int count = getTradeCount();
        if (!isValidProcess(process) || count <= 0) {
            return 0;
        }
        return Math.floorMod(processTradeIndices[process], count);
    }

    @Override
    public int getProgress(int process) {
        ensureProcessState();
        return isValidProcess(process) ? processProgress[process] : 0;
    }

    @Override
    public int getProgress() {
        ensureProcessState();
        int max = 0;
        for (int progress : processProgress) {
            max = Math.max(max, progress);
        }
        return max;
    }

    @Override
    public int getMaxProgress() {
        int configured = Config.tradeDurationTicks;
        return Math.max(MIN_TRADE_DURATION_TICKS, Math.min(MAX_TRADE_DURATION_TICKS,
                configured <= 0 ? 15 : configured));
    }

    @Override
    public boolean isSlotPaused(int process) {
        ensureProcessState();
        return isValidProcess(process) && processPaused[process];
    }

    @Override
    public FactoryStatus getProcessStatus(int process) {
        ensureProcessState();
        return isValidProcess(process) ? processStatuses[process] : FactoryStatus.IDLE;
    }

    public FactoryStatus getSlotStatus(int process) {
        return getProcessStatus(process);
    }

    private boolean isValidProcess(int process) {
        return process >= 0 && process < processProgress.length;
    }

    private void resetProcess(int process) {
        processProgress[process] = 0;
        processReservedInputs[process] = ItemStack.EMPTY;
        processReservedTradeIndices[process] = -1;
        processStatuses[process] = !tradeEnabled || processPaused[process]
                ? FactoryStatus.PAUSED : FactoryStatus.IDLE;
    }

    @Override
    public int getTradeCount() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            return 0;
        }
        try {
            return entity.getOffers().size();
        } catch (IllegalStateException clientSyncPending) {
            // Vanilla 1.21.1 deliberately refuses to lazily load offers on
            // the client. Inventory validation also runs during the client
            // click prediction path, so an unsynchronised villager must be
            // treated as having no offers until the server state arrives.
            return 0;
        }
    }

    @Override
    public int getTradeIndex() {
        int count = getTradeCount();
        if (count <= 0) {
            return 0;
        }
        return Math.floorMod(tradeIndex, count);
    }

    @Override
    public int getTradeUses() {
        MerchantOffer offer = getOffer();
        return offer == null ? 0 : offer.getUses();
    }

    @Override
    public int getTradeMaxUses() {
        MerchantOffer offer = getOffer();
        return offer == null ? 0 : offer.getMaxUses();
    }

    public int getTradeUses(int process) {
        MerchantOffer offer = getOffer(process);
        return offer == null ? 0 : offer.getUses();
    }

    public int getTradeMaxUses(int process) {
        MerchantOffer offer = getOffer(process);
        return offer == null ? 0 : offer.getMaxUses();
    }

    public boolean isTradeEnabled() {
        return tradeEnabled;
    }

    public void toggleTradeEnabled() {
        ensureProcessState();
        tradeEnabled = !tradeEnabled;
        for (int process = 0; process < processStatuses.length; process++) {
            processStatuses[process] = !tradeEnabled || processPaused[process]
                    ? FactoryStatus.PAUSED : FactoryStatus.IDLE;
        }
        sortingNeeded = true;
        setChanged();
        sync();
    }

    private void resetAllProcesses() {
        ensureProcessState();
        for (int process = 0; process < processProgress.length; process++) {
            resetProcess(process);
        }
        sortingNeeded = true;
    }

    @Override
    public int getEnergyUsage() {
        return scaledEnergy(Config.energyPerTrade);
    }

    @Nullable
    public MerchantOffer getOffer() {
        return getOfferForIndex(getTradeIndex());
    }

    @Nullable
    public MerchantOffer getOffer(int process) {
        return getOfferForIndex(getTradeIndex(process));
    }

    @Nullable
    private MerchantOffer getOfferForIndex(int index) {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            return null;
        }
        MerchantOffers offers;
        try {
            offers = entity.getOffers();
        } catch (IllegalStateException clientSyncPending) {
            return null;
        }
        if (index < 0 || index >= offers.size()) {
            return null;
        }
        return offers.get(index);
    }

    public ItemStack getAutoTradeInputA() {
        MerchantOffer offer = getOffer();
        return offer == null ? ItemStack.EMPTY : tradeCostA(offer);
    }

    public ItemStack getTradeCostB() {
        MerchantOffer offer = getOffer();
        return offer == null ? ItemStack.EMPTY : offer.getCostB().copy();
    }

    public ItemStack getTradeResult() {
        MerchantOffer offer = getOffer();
        return offer == null ? ItemStack.EMPTY : offer.getResult().copy();
    }

    public ItemStack getTradeCostA(int process) {
        MerchantOffer offer = getOffer(process);
        return offer == null ? ItemStack.EMPTY : tradeCostA(offer);
    }

    public ItemStack getTradeCostB(int process) {
        MerchantOffer offer = getOffer(process);
        return offer == null ? ItemStack.EMPTY : offer.getCostB().copy();
    }

    public ItemStack getTradeResult(int process) {
        MerchantOffer offer = getOffer(process);
        return offer == null ? ItemStack.EMPTY : offer.getResult().copy();
    }

    private void updateTradeInv() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity != null && level != null && !level.isClientSide) {
            entity.recalculateOffers();
        }
    }

    public MerchantOffers getOffersForSync() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null || level == null || level.isClientSide) {
            return new MerchantOffers();
        }
        try {
            return entity.getOffers().copy();
        } catch (IllegalStateException ignored) {
            return new MerchantOffers();
        }
    }

    @Override
    protected void work() {
        ensureProcessState();
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            setProcessStatus(FactoryStatus.NO_VILLAGER);
            status = FactoryStatus.NO_VILLAGER;
            return;
        }
        if (entity.isBaby()) {
            setProcessStatus(FactoryStatus.GROWING);
            status = FactoryStatus.GROWING;
            return;
        }
        if (!hasWorkstation()) {
            setProcessStatus(FactoryStatus.NO_WORKSTATION);
            status = FactoryStatus.NO_WORKSTATION;
            return;
        }
        if (!tradeEnabled) {
            setProcessStatus(FactoryStatus.PAUSED);
            status = FactoryStatus.PAUSED;
            return;
        }
        if (!entity.isTrading()
                && level.getGameTime() - lastRestockGameTime > nextRestock
                && entity.getVillagerData().getProfession().equals(getWorkstationProfession())) {
            restock(entity);
            lastRestockGameTime = level.getGameTime();
            nextRestock = calculateNextRestock();
        }

        // A process keeps its input visible while it is working. Validate the
        // saved reservation before sorting so a manually removed or replaced
        // input cannot leave a lane stuck with stale progress.
        resetInvalidProcesses();
        // Keep the distribution check alive while the mode is enabled. Some
        // menu/automation paths mutate a live ItemStack without firing the
        // handler listener; checking every tick prevents a stack from staying
        // stranded in the first lane. The planner is side-effect free when
        // the layout is already balanced and preserves running reservations.
        if (isAutoSort()) {
            sortingNeeded = true;
        }
        sortInputsIfNeeded();

        FactoryStatus aggregate = FactoryStatus.IDLE;
        boolean anyWorking = false;
        boolean anyUnpaused = false;
        boolean tradeCompleted = false;
        boolean changed = false;
        int duration = getMaxProgress();

        // A process owns exactly one input and one output slot. Count every
        // running lane, including paused lanes, as a reserved finite use so
        // parallel lanes cannot start more copies than the villager allows.
        Map<MerchantOffer, Integer> reservedUses = new java.util.IdentityHashMap<>();
        for (int process = 0; process < processProgress.length; process++) {
            if (processProgress[process] <= 0) {
                continue;
            }
            MerchantOffer offer = getOffer(process);
            if (offer != null && hasReservedTradeSelection(process)
                    && !hasInfiniteTradeUpgrade()) {
                reservedUses.merge(offer, 1, Integer::sum);
            }
        }

        for (int process = 0; process < processProgress.length; process++) {
            if (processPaused[process]) {
                processStatuses[process] = FactoryStatus.PAUSED;
                continue;
            }
            anyUnpaused = true;
            MerchantOffer offer = getOffer(process);
            if (offer == null) {
                if (processProgress[process] > 0) {
                    resetProcess(process);
                    changed = true;
                }
                processStatuses[process] = FactoryStatus.IDLE;
                continue;
            }

            if (processProgress[process] <= 0) {
                FactoryStatus blocked = checkStock(process, offer, reservedUses);
                if (blocked == null) {
                    blocked = checkTrade(process, offer);
                }
                if (blocked != null) {
                    processStatuses[process] = blocked;
                    if (aggregate == FactoryStatus.IDLE) {
                        aggregate = blocked;
                    }
                    continue;
                }
                ItemStack requiredInput = combinedTradeInput(offer);
                if (requiredInput == null || requiredInput.isEmpty()) {
                    processStatuses[process] = hasDifferentItemCosts(offer)
                            ? FactoryStatus.UNSUPPORTED_TRADE : FactoryStatus.NO_INPUT;
                    continue;
                }
                if (!hasInfiniteTradeUpgrade()) {
                    reservedUses.merge(offer, 1, Integer::sum);
                }
                int selectedTrade = getTradeIndex(process);
                processTradeIndices[process] = selectedTrade;
                processReservedInputs[process] = requiredInput.copy();
                processReservedTradeIndices[process] = selectedTrade;
                processProgress[process] = 1;
                processStatuses[process] = FactoryStatus.WORKING;
                anyWorking = true;
                changed = true;
                continue;
            }

            if (processProgress[process] < duration) {
                processProgress[process]++;
                processStatuses[process] = FactoryStatus.WORKING;
                anyWorking = true;
                changed = true;
            }
            if (processProgress[process] >= duration) {
                FactoryStatus blocked = completeTrade(process, offer, entity);
                if (blocked == null) {
                    releaseReservation(reservedUses, offer);
                    clearProcessReservation(process);
                    processProgress[process] = 0;
                    processStatuses[process] = FactoryStatus.WORKING;
                    anyWorking = true;
                    sortingNeeded = true;
                    changed = true;
                    tradeCompleted = true;
                } else {
                    // A failed completion is a failed attempt, not a paused
                    // operation. Release its finite-use reservation and return
                    // the lane to the normal start path. Keeping progress at
                    // duration would make the lane look active while it can no
                    // longer accept input or recover from a transient block.
                    releaseReservation(reservedUses, offer);
                    clearProcessReservation(process);
                    processProgress[process] = 0;
                    sortingNeeded = true;
                    changed = true;
                    processStatuses[process] = blocked;
                    if (aggregate == FactoryStatus.IDLE) {
                        aggregate = blocked;
                    }
                }
            }
        }
        if (changed) {
            setChanged();
        }
        if (tradeCompleted) {
            getVillager();
            sync();
            syncMenuSlots();
        }
        status = anyWorking ? FactoryStatus.WORKING
                : aggregate != FactoryStatus.IDLE ? aggregate : anyUnpaused ? FactoryStatus.IDLE : FactoryStatus.PAUSED;
    }

    private void resetInvalidProcesses() {
        boolean changed = false;
        for (int process = 0; process < processProgress.length; process++) {
            if (processProgress[process] <= 0) {
                continue;
            }
            MerchantOffer offer = getOffer(process);
            if (offer != null && hasReservedTradeSelection(process)
                    && hasReservedTradeInput(process)) {
                continue;
            }
            processProgress[process] = 0;
            clearProcessReservation(process);
            processStatuses[process] = processPaused[process] || !tradeEnabled
                    ? FactoryStatus.PAUSED : FactoryStatus.IDLE;
            changed = true;
        }
        if (changed) {
            sortingNeeded = true;
            setChanged();
        }
    }

    private static void releaseReservation(Map<MerchantOffer, Integer> reservedUses, MerchantOffer offer) {
        reservedUses.computeIfPresent(offer, (ignored, count) -> count <= 1 ? null : count - 1);
    }

    private void clearProcessReservation(int process) {
        processReservedInputs[process] = ItemStack.EMPTY;
        processReservedTradeIndices[process] = -1;
    }

    private boolean hasCurrentTradeSelection(int process) {
        int count = getTradeCount();
        return isValidProcess(process) && processTradeIndices[process] >= 0
                && processTradeIndices[process] < count;
    }

    private boolean hasReservedTradeSelection(int process) {
        return hasCurrentTradeSelection(process)
                && processReservedTradeIndices[process] == processTradeIndices[process];
    }

    @Nullable
    private FactoryStatus checkStock(int process, MerchantOffer offer,
                                     Map<MerchantOffer, Integer> reservedUses) {
        if (hasInfiniteTradeUpgrade()) {
            return null;
        }
        int reserved = reservedUses.getOrDefault(offer, 0);
        return offer.isOutOfStock() || offer.getUses() + reserved >= offer.getMaxUses()
                ? FactoryStatus.OUT_OF_STOCK : null;
    }

    /** Returns null when the process can begin or finish its own trade. */
    @Nullable
    private FactoryStatus checkTrade(int process, MerchantOffer offer) {
        if (!isValidProcess(process) || process >= inputItems.getSlots()) {
            return FactoryStatus.NO_INPUT;
        }
        ItemStack requiredInput = combinedTradeInput(offer);
        if (requiredInput == null || !extractExactly(inputItems, requiredInput, process, true)) {
            return FactoryStatus.NO_INPUT;
        }
        if (outputItems.insertItem(process, offer.getResult().copy(), true).getCount() > 0) {
            return FactoryStatus.OUTPUT_FULL;
        }
        if (getEnergy().getEnergyStored() < getEnergyUsage()) {
            return FactoryStatus.NO_ENERGY;
        }
        return null;
    }

    @Nullable
    private FactoryStatus completeTrade(int process, MerchantOffer offer, EasyVillagerEntity entity) {
        // Stock was reserved when this lane started. A different lane may
        // complete first and update the offer's dynamic use count, but that
        // must not invalidate an already reserved operation.
        ItemStack requiredInput = processReservedInputs[process];
        if (requiredInput == null || requiredInput.isEmpty()
                || !hasReservedTradeSelection(process)
                || !hasReservedTradeInput(process)) {
            return FactoryStatus.NO_INPUT;
        }
        FactoryStatus blocked = checkOutputAndEnergy(process, offer);
        if (blocked != null) {
            return blocked;
        }
        ItemStack result = offer.getResult().copy();
        ItemStack inputSnapshot = inputItems.getStackInSlot(process).copy();
        ItemStack outputSnapshot = outputItems.getStackInSlot(process).copy();
        int energySnapshot = getEnergy().getEnergyStored();

        // Consume only this process' input slot. A failed extraction restores
        // that slot, so a custom handler cannot swallow the lane's items or
        // overwrite changes made in another lane.
        if (requiredInput == null || !extractExactly(inputItems, requiredInput, process, false)) {
            restoreTradeState(process, inputSnapshot, outputSnapshot, energySnapshot);
            return FactoryStatus.NO_INPUT;
        }
        if (!consumeEnergy(getEnergyUsage())) {
            restoreTradeState(process, inputSnapshot, outputSnapshot, energySnapshot);
            return FactoryStatus.NO_ENERGY;
        }
        ItemStack remainder = outputItems.insertItem(process, result, false);
        if (!remainder.isEmpty()) {
            // The simulated insertion above should make this impossible, but do
            // not silently destroy an output if another handler changed the slot.
            restoreTradeState(process, inputSnapshot, outputSnapshot, energySnapshot);
            return FactoryStatus.OUTPUT_FULL;
        }
        if (!hasInfiniteTradeUpgrade()) {
            offer.increaseUses();
        }
        entity.setVillagerXp(entity.getVillagerXp() + offer.getXp());
        int villagerLevel = entity.getVillagerData().getLevel();
        if (VillagerData.canLevelUp(villagerLevel)
                && entity.getVillagerXp() >= VillagerData.getMaxXpPerLevel(villagerLevel)) {
            increaseCareer(entity);
        }
        return null;
    }

    @Nullable
    private FactoryStatus checkOutputAndEnergy(int process, MerchantOffer offer) {
        if (outputItems.insertItem(process, offer.getResult().copy(), true).getCount() > 0) {
            return FactoryStatus.OUTPUT_FULL;
        }
        if (getEnergy().getEnergyStored() < getEnergyUsage()) {
            return FactoryStatus.NO_ENERGY;
        }
        return null;
    }

    private void restoreTradeState(int process, ItemStack inputSnapshot, ItemStack outputSnapshot, int energySnapshot) {
        inputItems.setStackInSlot(process, inputSnapshot);
        outputItems.setStackInSlot(process, outputSnapshot);
        getEnergy().setEnergy(energySnapshot);
        setChanged();
    }

    /** Pushes direct handler mutations to every open menu for this factory. */
    private void syncMenuSlots() {
        if (batchingInventoryUpdate || level == null || level.isClientSide) {
            return;
        }
        for (Player player : level.players()) {
            if (player.containerMenu instanceof FactoryMenu menu
                    && (menu.getFactory() == this
                    || (menu.getFactory().getLevel() == level
                    && menu.getFactory().getBlockPos().equals(worldPosition)))) {
                // Handler-backed slots do not call slotsChanged when changed
                // by the block entity. Broadcast the complete state so newly
                // populated and emptied lanes are both visible immediately.
                menu.broadcastFullState();
            }
        }
    }

    private static ItemStack tradeCostA(MerchantOffer offer) {
        // getCostA is the villager's current price. Capping it to the base
        // price makes a discounted or increased offer disagree with the
        // actual trade operation and can strand a lane at completion.
        return offer.getCostA().copy();
    }

    /**
     * Each factory lane owns one input slot, so both offer costs must be the
     * same item before they can be consumed atomically. A null result marks an
     * offer that requires two different item types and therefore cannot run in
     * this one-slot-per-process layout.
     */
    @Nullable
    private static ItemStack combinedTradeInput(MerchantOffer offer) {
        ItemStack costA = tradeCostA(offer);
        ItemStack costB = offer.getCostB().copy();
        if (costA.isEmpty()) {
            return costB.isEmpty() ? null : costB;
        }
        if (costB.isEmpty()) {
            return costA;
        }
        if (!ItemStack.matches(costA, costB)) {
            return null;
        }
        int count = costA.getCount() + costB.getCount();
        if (count <= 0) {
            return null;
        }
        ItemStack combined = costA.copy();
        combined.setCount(count);
        return combined;
    }

    private static boolean hasDifferentItemCosts(MerchantOffer offer) {
        ItemStack costA = tradeCostA(offer);
        ItemStack costB = offer.getCostB().copy();
        return !costA.isEmpty() && !costB.isEmpty()
                && !ItemStack.matches(costA, costB);
    }

    private static boolean extractExactly(IItemHandler handler, ItemStack toMatch, int slot, boolean simulate) {
        if (toMatch.isEmpty()) {
            return true;
        }
        if (slot < 0 || slot >= handler.getSlots()) {
            return false;
        }
        ItemStack inSlot = handler.getStackInSlot(slot);
        if (inSlot.isEmpty() || !ItemStack.isSameItemSameComponents(inSlot, toMatch)
                || inSlot.getCount() < toMatch.getCount()) {
            return false;
        }
        ItemStack extracted = handler.extractItem(slot, toMatch.getCount(), simulate);
        return extracted.getCount() == toMatch.getCount()
                && ItemStack.isSameItemSameComponents(extracted, toMatch);
    }

    @Override
    protected void setProcessStatus(FactoryStatus processStatus) {
        ensureProcessState();
        for (int process = 0; process < processStatuses.length; process++) {
            processStatuses[process] = processPaused[process] ? FactoryStatus.PAUSED : processStatus;
        }
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        // SlotItemHandler reads directly from the block entity. Notify any
        // open server menu immediately when automation or a trade mutates a
        // stack; otherwise the client can keep showing the old lane.
        syncMenuSlots();
    }

    @Override
    protected void onInputChanged() {
        if (!sortingInputs) {
            sortingNeeded = true;
        }
    }

    /**
     * Vanilla quick-move can merge directly into an existing live stack and
     * only call Slot#setChanged, bypassing ItemStackHandler callbacks. Menus
     * use this hook to request the next distribution pass for that path.
     */
    public void markInputDirty() {
        sortingNeeded = true;
        // Quick-move's direct live-stack merge bypasses the handler callback,
        // so make sure the new count is persisted even when no redistribution
        // is needed.
        onInventoryChanged();
    }

    /**
     * Quick-move can shrink an output stack through the live ItemStack
     * returned by SlotItemHandler without invoking ItemStackHandler's
     * contents listener. Flush that mutation through the normal block/entity
     * and menu synchronization path.
     */
    public void markOutputDirty() {
        onInventoryChanged();
    }

    @Override
    protected void onAutoSortChanged() {
        sortingNeeded = true;
    }

    private void sortInputsIfNeeded() {
        if (!isAutoSort() || !sortingNeeded || inputItems.getSlots() < 2) {
            return;
        }
        ensureProcessState();
        int slotCount = inputItems.getSlots();
        ItemStack[] original = new ItemStack[slotCount];
        ItemStack[] sorted = new ItemStack[slotCount];
        Map<HashedItem, TradeInputDistribution> groups = new LinkedHashMap<>();
        boolean[] freeSlots = new boolean[slotCount];

        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = inputItems.getStackInSlot(slot).copy();
            original[slot] = stack;
            sorted[slot] = stack.copy();

            // A paused lane is explicitly owned by the player. Running lanes
            // remain eligible targets, but their captured input requirement is
            // treated as a hard minimum so redistribution cannot invalidate
            // the operation already in progress.
            if (processPaused[slot]) {
                continue;
            }
            if (stack.isEmpty()) {
                freeSlots[slot] = processProgress[slot] <= 0;
                continue;
            }

            // Group every non-empty source that some offer can consume. A
            // source may still be in the wrong lane because it was inserted by
            // an older version or before a trade selection changed; the target
            // planner below will only retain compatible lanes.
            // Never include an item that no offer can consume. Keeping such a
            // stack out of the plan is important: a failed redistribution must
            // leave it exactly where the player inserted it.
            if (!hasCompatibleTrade(stack)) {
                continue;
            }
            TradeInputDistribution group = groups.computeIfAbsent(HashedItem.raw(stack),
                    ignored -> new TradeInputDistribution(stack));
            group.totalCount += stack.getCount();
            group.sourceSlots.add(slot);
        }

        if (groups.isEmpty()) {
            sortingNeeded = false;
            return;
        }

        for (TradeInputDistribution group : groups.values()) {
            if (group.sourceSlots.isEmpty()) {
                continue;
            }

            int[] requirements = new int[slotCount];
            int[] limits = new int[slotCount];
            boolean[] running = new boolean[slotCount];
            for (int slot = 0; slot < slotCount; slot++) {
                if (group.sourceSlots.contains(slot) || freeSlots[slot]) {
                    requirements[slot] = minimumInputAmount(slot, group.stack);
                    limits[slot] = inputStackLimit(slot, group.stack);
                    running[slot] = processProgress[slot] > 0;
                }
            }
            int[] planned = TradeInputPlanner.plan(group.totalCount, requirements, limits, running);
            if (planned == null) {
                continue;
            }
            List<Integer> targets = new ArrayList<>();
            for (int slot = 0; slot < slotCount; slot++) {
                if (planned[slot] > 0) {
                    targets.add(slot);
                }
            }
            int[] amounts = targets.stream().mapToInt(slot -> planned[slot]).toArray();

            // The plan is complete for this group. Only now clear its sources
            // and reserve empty targets, so a later failed group cannot make an
            // earlier group lose its input.
            for (int source : group.sourceSlots) {
                sorted[source] = ItemStack.EMPTY;
                if (!targets.contains(source)) {
                    freeSlots[source] = true;
                }
            }
            for (int target : targets) {
                freeSlots[target] = false;
            }
            for (int index = 0; index < targets.size(); index++) {
                int target = targets.get(index);
                sorted[target] = group.stack.copyWithCount(amounts[index]);
            }
        }

        // Never commit a layout that changes the item multiset. This check is
        // deliberately independent of slot order and catches every possible
        // handler/limit edge case before a live stack is touched.
        if (!sameItemCounts(original, sorted)) {
            return;
        }

        boolean changed = false;
        for (int slot = 0; slot < slotCount; slot++) {
            if (!sameStack(original[slot], sorted[slot])) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            sortingNeeded = false;
            return;
        }

        // Automation can mutate the live handler between the snapshot and the
        // commit. Abandon this plan and retry on the next server tick instead
        // of overwriting the newer insertion.
        for (int slot = 0; slot < slotCount; slot++) {
            if (!sameStack(original[slot], inputItems.getStackInSlot(slot))) {
                return;
            }
        }

        sortingInputs = true;
        batchingInventoryUpdate = true;
        try {
            for (int slot = 0; slot < slotCount; slot++) {
                if (!sameStack(original[slot], sorted[slot])) {
                    inputItems.setStackInSlot(slot, sorted[slot].copy());
                }
            }
        } finally {
            batchingInventoryUpdate = false;
            sortingInputs = false;
        }
        sortingNeeded = false;
        setChanged();
        sync();
        syncMenuSlots();
    }

    private boolean hasCompatibleTrade(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            return false;
        }
        for (MerchantOffer offer : entity.getOffers()) {
            ItemStack required = combinedTradeInput(offer);
            if (required != null && ItemStack.isSameItemSameComponents(required, stack)) {
                return true;
            }
        }
        return false;
    }
    private boolean canAcceptDistribution(int process, ItemStack stack) {
        if (!isValidProcess(process) || processPaused[process]
                || processProgress[process] > 0 || stack.isEmpty()) {
            return false;
        }
        int required = requiredInputAmount(process, stack);
        return required > 0 && required <= stack.getMaxStackSize()
                && required <= inputItems.getSlotLimit(process);
    }

    private int inputStackLimit(int slot, ItemStack stack) {
        return Math.min(stack.getMaxStackSize() * getTier().stackMultiplier(), inputItems.getSlotLimit(slot));
    }

    private int minimumInputAmount(int process, ItemStack stack) {
        if (!isValidProcess(process) || stack.isEmpty()) {
            return 0;
        }
        if (processProgress[process] > 0) {
            ItemStack reserved = processReservedInputs[process];
            return reserved != null && !reserved.isEmpty()
                    && ItemStack.isSameItemSameComponents(reserved, stack) ? reserved.getCount() : 0;
        }
        return requiredInputAmount(process, stack);
    }

    private int requiredInputAmount(int process, ItemStack stack) {
        MerchantOffer offer = getOffer(process);
        if (offer == null || stack.isEmpty()) {
            return 0;
        }
        ItemStack requiredInput = combinedTradeInput(offer);
        if (requiredInput == null
                || !ItemStack.isSameItemSameComponents(requiredInput, stack)) {
            return 0;
        }
        return requiredInput.getCount();
    }

    /** Checks the input against the price captured when this lane started. */
    private boolean hasReservedTradeInput(int process) {
        if (!isValidProcess(process)) {
            return false;
        }
        ItemStack stack = inputItems.getStackInSlot(process);
        ItemStack requiredInput = processReservedInputs[process];
        if (stack.isEmpty() || requiredInput == null
                || !ItemStack.isSameItemSameComponents(stack, requiredInput)) {
            return false;
        }
        return stack.getCount() >= requiredInput.getCount();
    }

    private boolean canProcessInput(int process, ItemStack stack) {
        if (!isValidProcess(process) || processPaused[process] || requiredInputAmount(process, stack) <= 0) {
            return false;
        }
        MerchantOffer offer = getOffer(process);
        return offer != null
                && (hasInfiniteTradeUpgrade() || !offer.isOutOfStock())
                && outputItems.insertItem(process, offer.getResult().copy(), true).isEmpty();
    }

    private static final class TradeInputDistribution {
        private final ItemStack stack;
        private final List<Integer> sourceSlots = new ArrayList<>();
        private int totalCount;

        private TradeInputDistribution(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (first.isEmpty() && second.isEmpty()) {
            return true;
        }
        return first.getCount() == second.getCount() && ItemStack.matches(first, second);
    }

    private static boolean sameItemCounts(ItemStack[] first, ItemStack[] second) {
        Map<HashedItem, Integer> firstCounts = new HashMap<>();
        Map<HashedItem, Integer> secondCounts = new HashMap<>();
        for (ItemStack stack : first) {
            addItemCount(firstCounts, stack);
        }
        for (ItemStack stack : second) {
            addItemCount(secondCounts, stack);
        }
        return firstCounts.equals(secondCounts);
    }

    private static void addItemCount(Map<HashedItem, Integer> counts, ItemStack stack) {
        if (!stack.isEmpty()) {
            counts.merge(HashedItem.raw(stack.copyWithCount(1)), stack.getCount(), Integer::sum);
        }
    }

    private void increaseCareer(EasyVillagerEntity entity) {
        VillagerData data = entity.getVillagerData();
        int newLevel = data.getLevel() + 1;
        entity.setVillagerData(data.setLevel(newLevel));
        entity.recalculateOffers();
    }

    private void addTradesForLevel(EasyVillagerEntity entity, int level) {
        var map = VillagerTrades.TRADES.get(entity.getVillagerData().getProfession());
        if (map == null || map.isEmpty()) {
            return;
        }
        VillagerTrades.ItemListing[] listings = map.get(level);
        if (listings == null || listings.length == 0) {
            return;
        }
        MerchantOffers offers = entity.getOffers();
        java.util.Set<Integer> selected = new java.util.HashSet<>();
        if (listings.length > 2) {
            while (selected.size() < 2) {
                selected.add(entity.getRandom().nextInt(listings.length));
            }
        } else {
            for (int i = 0; i < listings.length; i++) {
                selected.add(i);
            }
        }
        for (int index : selected) {
            MerchantOffer generated = listings[index].getOffer(entity, entity.getRandom());
            if (generated != null) {
                offers.add(generated);
            }
        }
    }

    private void restock(EasyVillagerEntity entity) {
        try {
            entity.restock();
            SoundEvent workSound = entity.getVillagerData().getProfession().workSound();
            if (workSound != null) {
                playFactorySound(workSound, false);
            }
        } catch (Exception ignored) {
        }
    }

    private long calculateNextRestock() {
        int min = Main.SERVER_CONFIG.autoTraderMinRestockTime.get();
        int max = Main.SERVER_CONFIG.autoTraderMaxRestockTime.get();
        int low = Math.min(min, max);
        int high = Math.max(min, max);
        return low + level.random.nextInt(Math.max(1, high - low + 1));
    }

    @Override
    public void dropContents() {
        super.dropContents();
        if (hasWorkstation() && level != null) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, new ItemStack(workstation));
            workstation = Blocks.AIR;
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TraderFactoryMenu(id, inventory, this);
    }

    @Override
    protected void saveFactory(CompoundTag tag) {
        ensureProcessState();
        tag.putString("Workstation", BuiltInRegistries.BLOCK.getKey(workstation).toString());
        tag.putInt("TradeIndex", tradeIndex);
        tag.putBoolean("TradeEnabled", tradeEnabled);
        tag.putLong("NextRestock", nextRestock);
        tag.putLong("LastRestock", lastRestockGameTime);
        tag.putIntArray("TradeSelections", processTradeIndices);
        tag.putIntArray("TradeProgress", processProgress);
        tag.putIntArray("TradePaused", pausedAsInts());
        ListTag reservations = new ListTag();
        for (int process = 0; process < processProgress.length; process++) {
            ItemStack reserved = processReservedInputs[process];
            if (processProgress[process] <= 0 || reserved == null || reserved.isEmpty()
                    || processReservedTradeIndices[process] < 0) {
                continue;
            }
            CompoundTag reservation = new CompoundTag();
            reservation.putInt("Slot", process);
            reservation.putInt("TradeIndex", processReservedTradeIndices[process]);
            reservation.put("Input", reserved.save(level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY));
            reservations.add(reservation);
        }
        tag.put("TradeReservedInputs", reservations);
    }

    private int[] pausedAsInts() {
        int[] values = new int[processPaused.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = processPaused[i] ? 1 : 0;
        }
        return values;
    }

    @Override
    protected void loadFactory(CompoundTag tag) {
        ensureProcessState();
        String workstationId = tag.getString("Workstation");
        ResourceLocation parsedWorkstation = ResourceLocation.tryParse(workstationId);
        workstation = parsedWorkstation == null
                ? Blocks.AIR
                : BuiltInRegistries.BLOCK.getOptional(parsedWorkstation).orElse(Blocks.AIR);
        tradeIndex = tag.getInt("TradeIndex");
        tradeEnabled = !tag.contains("TradeEnabled") || tag.getBoolean("TradeEnabled");
        nextRestock = tag.getLong("NextRestock");
        lastRestockGameTime = tag.getLong("LastRestock");
        int[] selections = tag.contains("TradeSelections")
                ? tag.getIntArray("TradeSelections") : tag.getIntArray("TradeOverrides");
        int[] progress = tag.getIntArray("TradeProgress");
        int[] paused = tag.getIntArray("TradePaused");
        Arrays.fill(processReservedInputs, ItemStack.EMPTY);
        Arrays.fill(processReservedTradeIndices, -1);
        boolean hasReservations = tag.contains("TradeReservedInputs", Tag.TAG_LIST);
        if (hasReservations) {
            ListTag reservations = tag.getList("TradeReservedInputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < reservations.size(); i++) {
                CompoundTag reservation = reservations.getCompound(i);
                int process = reservation.getInt("Slot");
                if (!isValidProcess(process) || !reservation.contains("Input", Tag.TAG_COMPOUND)) {
                    continue;
                }
                ItemStack input = ItemStack.parse(level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY,
                        reservation.getCompound("Input")).orElse(ItemStack.EMPTY);
                int selected = reservation.getInt("TradeIndex");
                if (!input.isEmpty() && input.getCount() > 0 && selected >= 0) {
                    processReservedInputs[process] = input;
                    processReservedTradeIndices[process] = selected;
                }
            }
        }
        for (int i = 0; i < processProgress.length; i++) {
            // Older saves used -1 for "follow global". Migrate those lanes to
            // the concrete global selection used by the simplified UI.
            int selected = i < selections.length ? selections[i] : -1;
            processTradeIndices[i] = selected < 0 ? tradeIndex : selected;
            int loadedProgress = i < progress.length ? Math.max(0, Math.min(getMaxProgress(), progress[i])) : 0;
            // Before reservations were persisted, progress could survive a
            // reload without the input it represented. Clear only that stale
            // progress; the visible input remains available for a fresh run.
            if (loadedProgress > 0 && (!hasReservations
                    || processReservedInputs[i].isEmpty()
                    || processReservedTradeIndices[i] < 0)) {
                loadedProgress = 0;
            }
            processProgress[i] = loadedProgress;
            processPaused[i] = i < paused.length && paused[i] != 0;
            processStatuses[i] = !tradeEnabled || processPaused[i] ? FactoryStatus.PAUSED : FactoryStatus.IDLE;
        }
    }
}













