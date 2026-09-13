package com.dasien.mekv.blockentity;

import com.dasien.mekv.Config;
import com.dasien.mekv.factory.FactoryStatus;
import com.dasien.mekv.inventory.SingleStackHandler;
import com.dasien.mekv.menu.OutputFactoryMenu;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.util.FactoryHelper;
import de.maxhenkel.easyvillagers.Main;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SpecialPlantable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FarmerFactoryBlockEntity extends VillagerFactoryBlockEntity {
    private BlockState crop;
    private final IItemHandlerModifiable seedHandler;
    private int[] processProgress;
    private FactoryStatus[] processStatuses;

    public FarmerFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARMER_FACTORY.get(), pos, state, false);
        seedHandler = new SingleStackHandler(this::getSeedStack, this::setSeedFromStack, this::isValidSeedStack);
        setAutomationInputItems(seedHandler);
        initializeProcessState(getTier().processes());
    }

    public IItemHandler getSeedHandler() {
        return seedHandler;
    }

    public ItemStack getSeedStack() {
        return crop == null ? ItemStack.EMPTY : new ItemStack(crop.getBlock().asItem());
    }

    public void setSeedFromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            setCrop(null);
            return;
        }
        if (isValidSeed(stack.getItem())) {
            setCrop(stack.getItem());
        }
    }

    public boolean isValidSeedStack(ItemStack stack) {
        return !stack.isEmpty() && isValidSeed(stack.getItem());
    }

    @Override
    protected void onAddVillager(EasyVillagerEntity entity) {
        if (entity.getVillagerXp() <= 0 && !entity.getVillagerData().getProfession().equals(VillagerProfession.NITWIT)) {
            entity.setVillagerData(entity.getVillagerData().setProfession(VillagerProfession.FARMER));
        }
    }

    @Nullable
    public BlockState getCrop() {
        return crop;
    }

    public void setCrop(@Nullable Item seed) {
        crop = seed == null ? null : getSeedCrop(seed);
        initializeProcessState(getTier().processes());
        setChanged();
        sync();
    }

    @Nullable
    public Block removeSeed() {
        if (crop == null) {
            return null;
        }
        Block block = crop.getBlock();
        setCrop(null);
        return block;
    }

    public boolean isValidSeed(Item item) {
        return getSeedCrop(item) != null;
    }

    @Nullable
    public BlockState getSeedCrop(Item item) {
        if (item == Items.WHEAT_SEEDS) {
            return Blocks.WHEAT.defaultBlockState();
        }
        if (item == Items.POTATO) {
            return Blocks.POTATOES.defaultBlockState();
        }
        if (item == Items.CARROT) {
            return Blocks.CARROTS.defaultBlockState();
        }
        if (item == Items.BEETROOT_SEEDS) {
            return Blocks.BEETROOTS.defaultBlockState();
        }
        return null;
    }

    @Override
    public int getProgress() {
        int progress = 0;
        for (int value : processProgress) {
            progress = Math.max(progress, value);
        }
        return progress;
    }

    @Override
    public int getEnergyUsage() {
        return scaledEnergy(Config.energyPerCropAge);
    }

    @Override
    public int getMaxProgress() {
        IntegerProperty age = ageProperty();
        if (age == null) {
            return 1;
        }
        return age.getPossibleValues().stream().max(Comparator.naturalOrder()).orElse(1);
    }

    @Override
    public int getProcessCount() {
        return processProgress.length;
    }

    @Override
    public int getProgress(int process) {
        return process < 0 || process >= processProgress.length ? 0 : processProgress[process];
    }

    @Override
    public FactoryStatus getProcessStatus(int process) {
        return process < 0 || process >= processStatuses.length
                ? FactoryStatus.IDLE : processStatuses[process];
    }

    @Override
    protected void setProcessStatus(FactoryStatus processStatus) {
        Arrays.fill(processStatuses, processStatus);
    }

    private void initializeProcessState(int count) {
        processProgress = new int[Math.max(0, count)];
        processStatuses = new FactoryStatus[processProgress.length];
        Arrays.fill(processStatuses, FactoryStatus.IDLE);
    }

    @Nullable
    private IntegerProperty ageProperty() {
        if (crop == null) {
            return null;
        }
        return crop.getProperties().stream()
                .filter(IntegerProperty.class::isInstance)
                .map(IntegerProperty.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void work() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            status = FactoryStatus.NO_VILLAGER;
            setProcessStatus(status);
            return;
        }
        if (crop == null) {
            status = FactoryStatus.NO_INPUT;
            setProcessStatus(status);
            return;
        }
        // Easy Villagers checks every 20 ticks. Scale that check interval once
        // for speed upgrades and keep the original random farm-speed chance;
        // this preserves the default rate without applying the multiplier
        // twice.
        int interval = scaledTicks(20);
        int farmSpeed = Math.max(1, Main.SERVER_CONFIG.farmSpeed.get());
        boolean anyWorking = false;
        FactoryStatus aggregate = FactoryStatus.IDLE;
        for (int process = 0; process < processProgress.length; process++) {
            if (level.getGameTime() % interval != 0 || level.random.nextInt(farmSpeed) != 0) {
                continue;
            }
            if (processProgress[process] >= getMaxProgress()) {
                if (finishCycle(process, entity)) {
                    anyWorking = true;
                } else {
                    aggregate = FactoryStatus.OUTPUT_FULL;
                }
                continue;
            }
            if (!consumeEnergy(getEnergyUsage())) {
                processStatuses[process] = FactoryStatus.NO_ENERGY;
                if (aggregate == FactoryStatus.IDLE) {
                    aggregate = FactoryStatus.NO_ENERGY;
                }
                continue;
            }
            processProgress[process]++;
            processStatuses[process] = FactoryStatus.WORKING;
            anyWorking = true;
            setChanged();
        }
        status = anyWorking ? FactoryStatus.WORKING : aggregate;
        if (anyWorking) {
            sync();
        }
    }

    private boolean finishCycle(int process, EasyVillagerEntity entity) {
        IntegerProperty age = ageProperty();
        if (crop == null || age == null) {
            processStatuses[process] = FactoryStatus.IDLE;
            return false;
        }
        if (entity.isBaby()) {
            processStatuses[process] = FactoryStatus.GROWING;
            return false;
        }
        if (!entity.getVillagerData().getProfession().equals(VillagerProfession.FARMER)) {
            processStatuses[process] = FactoryStatus.IDLE;
            return false;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        int max = age.getPossibleValues().stream().max(Comparator.naturalOrder()).orElse(0);
        List<ItemStack> drops = getHarvestDrops(serverLevel, crop.setValue(age, max));
        if (!canFullyInsertProcessDrops(process, drops)) {
            processStatuses[process] = FactoryStatus.OUTPUT_FULL;
            return false;
        }
        if (!consumeEnergy(scaledEnergy(Config.energyPerHarvest))) {
            processStatuses[process] = FactoryStatus.NO_ENERGY;
            return false;
        }
        for (ItemStack drop : drops) {
            insertProcessDrop(process, drop);
        }
        processProgress[process] = 0;
        processStatuses[process] = FactoryStatus.WORKING;
        playFactorySound(SoundEvents.VILLAGER_WORK_FARMER, false);
        setChanged();
        return true;
    }

    private boolean canFullyInsertProcessDrops(int process, List<ItemStack> drops) {
        net.neoforged.neoforge.items.ItemStackHandler copy = new com.dasien.mekv.inventory.FactoryStackHandler(2, getTier().stackMultiplier());
        copy.setStackInSlot(0, outputItems.getStackInSlot(process * 2).copy());
        copy.setStackInSlot(1, outputItems.getStackInSlot(process * 2 + 1).copy());
        for (ItemStack drop : drops) {
            if (!FactoryHelper.canFullyInsert(copy, drop)) {
                return false;
            }
            FactoryHelper.insertAll(copy, drop.copy(), false);
        }
        return true;
    }

    private void insertProcessDrop(int process, ItemStack drop) {
        int first = process * 2;
        ItemStack remainder = outputItems.insertItem(first, drop, false);
        if (!remainder.isEmpty()) {
            remainder = outputItems.insertItem(first + 1, remainder, false);
        }
        if (!remainder.isEmpty() && level != null) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, remainder);
        }
    }

    private List<ItemStack> getHarvestDrops(ServerLevel serverLevel, BlockState state) {
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
        return state.getDrops(builder);
    }

    @Override
    public void dropContents() {
        super.dropContents();
        if (crop != null && level != null) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, getSeedStack());
            crop = null;
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OutputFactoryMenu(id, inventory, this);
    }

    @Override
    protected void saveFactory(CompoundTag tag) {
        if (crop != null) {
            tag.put("Crop", NbtUtils.writeBlockState(crop));
        }
        tag.putIntArray("FarmerProgress", processProgress);
    }

    @Override
    protected void loadFactory(CompoundTag tag) {
        if (tag.contains("Crop")) {
            crop = NbtUtils.readBlockState(level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK)
                    : net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("Crop"));
        } else {
            crop = null;
        }
        int[] loaded = tag.getIntArray("FarmerProgress");
        for (int process = 0; process < processProgress.length; process++) {
            processProgress[process] = crop != null && process < loaded.length
                    ? Math.max(0, Math.min(getMaxProgress(), loaded[process])) : 0;
            processStatuses[process] = FactoryStatus.IDLE;
        }
    }

}












