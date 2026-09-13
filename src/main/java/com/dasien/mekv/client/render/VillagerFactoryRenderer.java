package com.dasien.mekv.client.render;

import com.dasien.mekv.blockentity.FarmerFactoryBlockEntity;
import com.dasien.mekv.blockentity.IronGolemFactoryBlockEntity;
import com.dasien.mekv.blockentity.TraderFactoryBlockEntity;
import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class VillagerFactoryRenderer implements BlockEntityRenderer<VillagerFactoryBlockEntity> {
    private IronGolem cachedGolem;
    private Zombie cachedZombie;

    public VillagerFactoryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VillagerFactoryBlockEntity factory, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (factory instanceof IronGolemFactoryBlockEntity ironFarm) {
            renderIronFarm(ironFarm, poseStack, buffer, packedLight);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Direction facing = factory.getFacing();
        int light = LightTexture.FULL_BRIGHT;
        EasyVillagerEntity villager = factory.getVillagerEntity();
        if (villager != null) {
            syncRenderTick(villager, factory.getLevel());
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.135d, 0.5d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // Villagers sit behind the workstation, toward the rear wall.
            poseStack.translate(0.0d, 0.0d, -0.25d);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            mc.getEntityRenderDispatcher().render(villager, 0.0d, 0.0d, 0.0d, 0.0f, 0.0f, poseStack, buffer, light);
            poseStack.popPose();
        }

        if (factory instanceof TraderFactoryBlockEntity trader && trader.hasWorkstation()) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.135d, 0.5d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // Workstations sit toward the front window, ahead of the villager.
            poseStack.translate(0.0d, 0.0d, 0.125d);
            poseStack.translate(-0.5d, 0.0d, -0.5d);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            // Center the scaled workstation on the window's X axis. The
            // previous offset left it visibly displaced after rotation.
            poseStack.translate(0.75d, 0.0d, 0.6111111111111112d);
            BlockState workstation = fittingState(trader.getWorkstation().defaultBlockState());
            mc.getBlockRenderer().renderSingleBlock(workstation, poseStack, buffer, light, packedOverlay);
            poseStack.popPose();
        }

        if (factory instanceof FarmerFactoryBlockEntity farmer && farmer.getCrop() != null) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.135d, 0.5d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // Crops are displayed at the front of the machine; the villager
            // is rendered separately behind them.
            poseStack.translate(0.0d, 0.0d, 0.125d);
            poseStack.translate(-0.5d, 0.0d, -0.5d);
            poseStack.scale(0.45f, 0.45f, 0.45f);
            poseStack.translate(0.6111111111111112d, 0.0d, 0.6111111111111112d);
            mc.getBlockRenderer().renderSingleBlock(farmer.getCrop(), poseStack, buffer, light, packedOverlay);
            poseStack.popPose();
        }

    }

    private void renderIronFarm(IronGolemFactoryBlockEntity factory, PoseStack poseStack,
                                MultiBufferSource buffer, int packedLight) {
        Level level = factory.getLevel();
        if (level == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Direction facing = factory.getFacing();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        int light = LightTexture.FULL_BRIGHT;
        EasyVillagerEntity villager = factory.getVillagerEntity();
        if (villager != null) {
            syncRenderTick(villager, level);
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.135d, 0.5d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // Keep the villager on the rear metal floor with clearance from
            // the machine's back wall.
            poseStack.translate(-0.3125d, 0.0d, -0.125d);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));
            poseStack.scale(0.3f, 0.3f, 0.3f);
            dispatcher.render(villager, 0.0d, 0.0d, 0.0d, 0.0f, 0.0f, poseStack, buffer, light);
            poseStack.popPose();
        }

        Zombie zombie = zombie(level);
        if (zombie != null) {
            syncRenderTick(zombie, level);
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.135d, 0.5d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            // The zombie shares the rear metal floor with the villager.
            poseStack.translate(0.3125d, 0.0d, -0.125d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
            poseStack.scale(0.3f, 0.3f, 0.3f);
            dispatcher.render(zombie, 0.0d, 0.0d, 0.0d, 0.0f, 0.0f, poseStack, buffer, light);
            poseStack.popPose();
        }

        if (factory.isGolemVisible()) {
            IronGolem golem = golem(level);
            if (golem != null) {
                syncRenderTick(golem, level);
                golem.hurtTime = factory.getTimer() % 20 < 10 ? 20 : 0;
                poseStack.pushPose();
                poseStack.translate(0.5d, 0.22d, 0.5d);
                poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                poseStack.translate(0.0d, 0.0d, 0.1875d);
                poseStack.scale(0.3f, 0.3f, 0.3f);
                dispatcher.render(golem, 0.0d, 0.0d, 0.0d, 0.0f, 0.0f, poseStack, buffer, light);
                poseStack.popPose();
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(VillagerFactoryBlockEntity factory) {
        return true;
    }

    private IronGolem golem(Level level) {
        if (cachedGolem == null || cachedGolem.level() != level) {
            cachedGolem = EntityType.IRON_GOLEM.create(level);
        }
        return cachedGolem;
    }

    private Zombie zombie(Level level) {
        if (cachedZombie == null || cachedZombie.level() != level) {
            cachedZombie = EntityType.ZOMBIE.create(level);
        }
        return cachedZombie;
    }

    private static void syncRenderTick(net.minecraft.world.entity.Entity entity, Level level) {
        if (level != null) {
            entity.tickCount = (int) level.getGameTime();
        }
    }

    private static BlockState fittingState(BlockState state) {
        if (state.getBlock() instanceof GrindstoneBlock) {
            return state.setValue(GrindstoneBlock.FACE, AttachFace.FLOOR);
        }
        return state;
    }
}
