package com.dasien.mekv.blockentity;

import com.dasien.mekv.Config;
import com.dasien.mekv.factory.FactoryStatus;
import com.dasien.mekv.menu.OutputFactoryMenu;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.util.FactoryHelper;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IronGolemFactoryBlockEntity extends VillagerFactoryBlockEntity {
    private static final ResourceLocation GOLEM_LOOT = ResourceLocation.fromNamespaceAndPath("minecraft", "entities/iron_golem");
    private long[] processTimers;
    private FactoryStatus[] processStatuses;

    public IronGolemFactoryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_GOLEM_FACTORY.get(), pos, state, false);
        setAutomationInputItems(villagerHandler);
        initializeProcessState(getTier().processes());
    }

    public long getTimer() {
        long timer = 0;
        for (long processTimer : processTimers) {
            timer = Math.max(timer, processTimer);
        }
        return timer;
    }

    public int getGolemSpawnTime() {
        return Math.max(1, getGolemKillTime() - scaledTicks(40));
    }

    public int getGolemKillTime() {
        return scaledTicks(100);
    }

    @Override
    public int getEnergyUsage() {
        return scaledOperationEnergy(Config.energyPerGolem);
    }

    @Override
    public int getProgress() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, getTimer()));
    }

    @Override
    public int getMaxProgress() {
        return Math.max(1, getGolemKillTime());
    }

    public boolean isGolemVisible() {
        if (!hasVillager()) {
            return false;
        }
        for (long timer : processTimers) {
            if (timer >= getGolemSpawnTime() && timer < getGolemKillTime()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getProcessCount() {
        return processTimers.length;
    }

    @Override
    public int getProgress(int process) {
        if (process < 0 || process >= processTimers.length) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, processTimers[process]));
    }

    @Override
    public FactoryStatus getProcessStatus(int process) {
        if (process < 0 || process >= processStatuses.length) {
            return FactoryStatus.IDLE;
        }
        return processStatuses[process];
    }

    @Override
    protected void setProcessStatus(FactoryStatus processStatus) {
        Arrays.fill(processStatuses, processStatus);
    }

    private void initializeProcessState(int count) {
        processTimers = new long[Math.max(0, count)];
        processStatuses = new FactoryStatus[processTimers.length];
        Arrays.fill(processStatuses, FactoryStatus.IDLE);
    }

    @Override
    protected void work() {
        EasyVillagerEntity entity = getVillagerEntity();
        if (entity == null) {
            status = FactoryStatus.NO_VILLAGER;
            setProcessStatus(status);
            return;
        }
        if (level.getGameTime() % 200 == 0) {
            playFactorySound(SoundEvents.ZOMBIE_AMBIENT, true);
        }
        int spawn = getGolemSpawnTime();
        int kill = getGolemKillTime();
        boolean anyWorking = false;
        FactoryStatus aggregate = FactoryStatus.IDLE;
        for (int process = 0; process < processTimers.length; process++) {
            long timer = processTimers[process];
            if (timer >= kill) {
                if (finishCycle(process)) {
                    anyWorking = true;
                } else {
                    aggregate = FactoryStatus.OUTPUT_FULL;
                }
                continue;
            }
            if (!consumeEnergy(energyDueForNextTick(timer, kill))) {
                processStatuses[process] = FactoryStatus.NO_ENERGY;
                if (aggregate == FactoryStatus.IDLE) {
                    aggregate = FactoryStatus.NO_ENERGY;
                }
                continue;
            }
            processTimers[process] = ++timer;
            processStatuses[process] = FactoryStatus.WORKING;
            anyWorking = true;
            setChanged();
            if (timer == spawn) {
                playFactorySound(SoundEvents.ZOMBIE_AMBIENT, false);
            } else if (timer > spawn && timer < kill && timer % 20 == 0) {
                playFactorySound(SoundEvents.IRON_GOLEM_HURT, false);
            }
        }
        status = anyWorking ? FactoryStatus.WORKING : aggregate;
        if (anyWorking) {
            sync();
        }
    }

    private boolean finishCycle(int process) {
        List<ItemStack> drops = getDrops();
        if (!canFullyInsertProcessDrops(process, drops)) {
            processStatuses[process] = FactoryStatus.OUTPUT_FULL;
            return false;
        }
        playFactorySound(SoundEvents.IRON_GOLEM_DEATH, false);
        for (ItemStack drop : drops) {
            insertProcessDrop(process, drop);
        }
        processTimers[process] = 0;
        processStatuses[process] = FactoryStatus.WORKING;
        setChanged();
        return true;
    }

    private boolean canFullyInsertProcessDrops(int process, List<ItemStack> drops) {
        ItemStackHandler copy = new com.dasien.mekv.inventory.FactoryStackHandler(2, getTier().stackMultiplier());
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
            // Never silently discard a remainder if the live handler changed
            // between the capacity check and the commit.
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, remainder);
        }
    }

    private int energyDueForNextTick(long timer, int kill) {
        int total = getEnergyUsage();
        if (total <= 0 || kill <= 0) {
            return 0;
        }
        int paid = (int) ((timer * total) / kill);
        int next = (int) (((timer + 1L) * total) / kill);
        return Math.max(0, next - paid);
    }

    private List<ItemStack> getDrops() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }
        IronGolem golem = new IronGolem(EntityType.IRON_GOLEM, level);
        LootParams.Builder builder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, golem)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.DAMAGE_SOURCE, serverLevel.damageSources().generic());
        return serverLevel.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, GOLEM_LOOT))
                .getRandomItems(builder.create(LootContextParamSets.ENTITY));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OutputFactoryMenu(id, inventory, this);
    }

    @Override
    protected void saveFactory(CompoundTag tag) {
        long[] timers = processTimers;
        if (timers.length > 0) {
            tag.putLongArray("GolemProgress", timers);
        }
    }

    @Override
    protected void loadFactory(CompoundTag tag) {
        long[] loaded = tag.getLongArray("GolemProgress");
        if (loaded.length == 0 && tag.contains("Timer")) {
            loaded = new long[]{tag.getLong("Timer")};
        }
        for (int process = 0; process < processTimers.length; process++) {
            processTimers[process] = process < loaded.length
                    ? Math.min(getGolemKillTime(), Math.max(0, loaded[process])) : 0;
            processStatuses[process] = processTimers[process] >= getGolemKillTime()
                    ? FactoryStatus.OUTPUT_FULL : FactoryStatus.IDLE;
        }
    }
}












