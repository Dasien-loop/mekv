package com.dasien.mekv.block;

import com.dasien.mekv.blockentity.FarmerFactoryBlockEntity;
import com.dasien.mekv.blockentity.IronGolemFactoryBlockEntity;
import com.dasien.mekv.blockentity.TraderFactoryBlockEntity;
import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.util.FactoryInteraction;
import com.dasien.mekv.util.FactoryHelper;
import de.maxhenkel.easyvillagers.blocks.VillagerBlockBase;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import mekanism.common.item.ItemConfigurationCard;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.util.SecurityUtils;
import mekanism.api.text.ILangEntry;
import mekanism.common.block.interfaces.IHasDescription;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;


public class VillagerFactoryBlock extends HorizontalDirectionalBlock implements EntityBlock, IHasDescription {
    private static final VoxelShape SHAPE = Shapes.block();
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private final VillagerFactoryType factoryType;
    private final FactoryTier tier;

    public VillagerFactoryBlock(VillagerFactoryType factoryType, FactoryTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5f)
                .sound(SoundType.METAL)
                .noOcclusion());
        this.factoryType = factoryType;
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    public VillagerFactoryType getFactoryType() {
        return factoryType;
    }

    public FactoryTier getTier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(ACTIVE, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public ILangEntry getDescription() {
        return () -> "description.mekv.factory." + factoryType.getSerializedName();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof VillagerFactoryBlockEntity factory)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof ItemConfigurationCard) {
            return InteractionResult.PASS;
        }
        if (held.getItem() instanceof ItemConfigurator configurator) {
            ItemConfigurator.ConfiguratorMode mode = configurator.getMode(held);
            if (mode.isConfigurating()) {
                return InteractionResult.PASS;
            }
            if (mode == ItemConfigurator.ConfiguratorMode.ROTATE || mode == ItemConfigurator.ConfiguratorMode.WRENCH) {
                if (!factory.canAccess(player)) {
                    SecurityUtils.get().displayNoAccess(player);
                    return InteractionResult.FAIL;
                }
                if (!level.isClientSide) {
                    if (mode == ItemConfigurator.ConfiguratorMode.WRENCH && player.isShiftKeyDown()) {
                        ItemStack sustained = factory.createSustainedStack();
                        factory.prepareForSustainedRemoval();
                        level.removeBlock(pos, false);
                        FactoryHelper.giveToPlayer(level, pos, state, player, hand, sustained);
                    } else if (mode == ItemConfigurator.ConfiguratorMode.ROTATE
                            && hit.getDirection().getAxis().isHorizontal()) {
                        Direction direction = player.isShiftKeyDown() ? hit.getDirection().getOpposite() : hit.getDirection();
                        level.setBlock(pos, state.setValue(FACING, direction), 3);
                    } else {
                        level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!factory.canAccess(player)) {
            if (!level.isClientSide) {
                SecurityUtils.get().displayNoAccess(player);
            }
            return InteractionResult.FAIL;
        }
        if (held.getItem() instanceof ItemTierInstaller installer) {
            return FactoryInteraction.useTierInstaller(installer, new UseOnContext(player, hand, hit));
        }
        InteractionResult extraInstaller = FactoryInteraction.useExtraTierInstaller(held.getItem(), new UseOnContext(player, hand, hit));
        if (extraInstaller != InteractionResult.PASS) {
            return extraInstaller;
        }
        if (player.isShiftKeyDown() && factory.isSupportedUpgrade(held)) {
            return FactoryInteraction.useUpgrade(held, new UseOnContext(player, hand, hit));
        }
        return switch (factoryType) {
            case TRADER -> useTrader(state, level, pos, player, hand, factory);
            case FARMER -> useFarmer(state, level, pos, player, hand, factory);
            case IRON_GOLEM -> useIronGolem(state, level, pos, player, hand, factory);
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof VillagerFactoryBlockEntity factory) {
            if (placer instanceof Player player) {
                factory.setOwner(player);
            }
            if (stack.hasCustomHoverName()) {
                factory.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VillagerFactoryBlockEntity factory
                ? factory.getComparatorLevel() : 0;
    }

    private InteractionResult useTrader(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, VillagerFactoryBlockEntity factory) {
        if (!(factory instanceof TraderFactoryBlockEntity trader)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!trader.hasVillager() && held.getItem() instanceof VillagerItem) {
            if (!level.isClientSide) {
                trader.setVillager(held.copy());
                held.shrink(player.getAbilities().instabuild ? 0 : 1);
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            }
            return InteractionResult.SUCCESS;
        }
        if (!trader.hasWorkstation() && held.getItem() instanceof BlockItem blockItem && trader.isValidWorkstation(blockItem.getBlock())) {
            if (!level.isClientSide) {
                trader.setWorkstation(blockItem.getBlock());
                held.shrink(player.getAbilities().instabuild ? 0 : 1);
                SoundType soundType = blockItem.getBlock().defaultBlockState().getSoundType();
                FactoryHelper.play(level, pos, soundType.getPlaceSound());
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && trader.hasVillager()) {
            if (!level.isClientSide) {
                FactoryHelper.giveToPlayer(level, pos, state, player, hand, trader.removeVillager());
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && trader.hasWorkstation()) {
            if (!level.isClientSide) {
                FactoryHelper.giveToPlayer(level, pos, state, player, hand, new ItemStack(trader.removeWorkstation()));
            }
            return InteractionResult.SUCCESS;
        }
        return openGui(level, pos, player, trader);
    }

    private InteractionResult useFarmer(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, VillagerFactoryBlockEntity factory) {
        if (!(factory instanceof FarmerFactoryBlockEntity farmer)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!farmer.hasVillager() && held.getItem() instanceof VillagerItem) {
            if (!level.isClientSide) {
                farmer.setVillager(held.copy());
                held.shrink(player.getAbilities().instabuild ? 0 : 1);
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_YES);
            }
            return InteractionResult.SUCCESS;
        }
        if (farmer.getCrop() == null && farmer.isValidSeed(held.getItem())) {
            if (!level.isClientSide) {
                farmer.setCrop(held.getItem());
                held.shrink(player.getAbilities().instabuild ? 0 : 1);
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.CROP_PLANTED);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && farmer.getCrop() != null) {
            if (!level.isClientSide) {
                Block seed = farmer.removeSeed();
                if (seed != null) {
                    FactoryHelper.giveToPlayer(level, pos, state, player, hand, new ItemStack(seed));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && farmer.hasVillager()) {
            if (!level.isClientSide) {
                FactoryHelper.giveToPlayer(level, pos, state, player, hand, farmer.removeVillager());
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            }
            return InteractionResult.SUCCESS;
        }
        return openGui(level, pos, player, farmer);
    }

    private InteractionResult useIronGolem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, VillagerFactoryBlockEntity factory) {
        if (!(factory instanceof IronGolemFactoryBlockEntity ironFarm)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!ironFarm.hasVillager() && held.getItem() instanceof VillagerItem) {
            if (!level.isClientSide) {
                ironFarm.setVillager(held.copy());
                held.shrink(player.getAbilities().instabuild ? 0 : 1);
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && ironFarm.hasVillager()) {
            if (!level.isClientSide) {
                FactoryHelper.giveToPlayer(level, pos, state, player, hand, ironFarm.removeVillager());
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            }
            return InteractionResult.SUCCESS;
        }
        return openGui(level, pos, player, ironFarm);
    }

    private InteractionResult openGui(Level level, BlockPos pos, Player player, VillagerFactoryBlockEntity factory) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, factory, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VillagerFactoryBlockEntity factory
                && !factory.isTierUpgradeInProgress()) {
            factory.dropContents();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (factoryType) {
            case TRADER -> new TraderFactoryBlockEntity(pos, state);
            case IRON_GOLEM -> new IronGolemFactoryBlockEntity(pos, state);
            case FARMER -> new FarmerFactoryBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (lvl, pos, blockState, be) -> {
                    if (be instanceof VillagerFactoryBlockEntity factory) {
                        VillagerFactoryBlockEntity.clientTick(lvl, pos, blockState, factory);
                    }
                }
                : (lvl, pos, blockState, be) -> {
                    if (be instanceof VillagerFactoryBlockEntity factory) {
                        VillagerFactoryBlockEntity.serverTick(lvl, pos, blockState, factory);
                    }
                };
    }
}
