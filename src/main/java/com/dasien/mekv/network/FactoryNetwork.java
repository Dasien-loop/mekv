package com.dasien.mekv.network;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.inventory.FactoryStackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

public final class FactoryNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Mekv.MODID, "factory"), () -> "1", "1"::equals, "1"::equals);

    private FactoryNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, LargeSlot.class, LargeSlot::encode, LargeSlot::decode, LargeSlot::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, MenuButton.class, MenuButton::encode, MenuButton::decode, MenuButton::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void clickButton(int containerId, int button) {
        CHANNEL.sendToServer(new MenuButton(containerId, button));
    }

    public static void sendSlot(ServerPlayer player, AbstractContainerMenu menu, int slot, ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new LargeSlot(menu.containerId, menu.incrementStateId(), slot, FactoryStackHandler.saveStack(stack)));
    }

    public record LargeSlot(int containerId, int stateId, int slot, CompoundTag stack) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(stateId);
            buffer.writeVarInt(slot);
            buffer.writeNbt(stack);
        }

        private static LargeSlot decode(FriendlyByteBuf buffer) {
            return new LargeSlot(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readNbt());
        }

        private void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> com.dasien.mekv.client.FactoryPacketHandler.handle(this));
            context.get().setPacketHandled(true);
        }
    }

    public record MenuButton(int containerId, int button) {
        public void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeVarInt(button);
        }

        public static MenuButton decode(FriendlyByteBuf buffer) {
            return new MenuButton(buffer.readVarInt(), buffer.readVarInt());
        }

        private void handle(Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                ServerPlayer player = context.get().getSender();
                if (player != null && player.containerMenu instanceof com.dasien.mekv.menu.FactoryMenu menu
                        && menu.containerId == containerId && menu.stillValid(player) && !player.isSpectator()) {
                    menu.clickMenuButton(player, button);
                    menu.broadcastChanges();
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}
