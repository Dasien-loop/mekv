package com.dasien.mekv.network;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.inventory.FactoryStackHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class FactoryNetwork {
    private static final String VERSION = "1";

    private FactoryNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION)
                .playToClient(LargeSlot.TYPE, LargeSlot.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> com.dasien.mekv.client.FactoryPacketHandler.handle(payload)))
                .playToClient(TradeOffers.TYPE, TradeOffers.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> com.dasien.mekv.client.FactoryPacketHandler.handle(payload)))
                .playToServer(MenuButton.TYPE, MenuButton.STREAM_CODEC, (payload, context) ->
                        context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player
                                    && player.containerMenu instanceof com.dasien.mekv.menu.FactoryMenu menu
                                    && menu.containerId == payload.containerId()
                                    && menu.stillValid(player) && !player.isSpectator()) {
                                menu.clickMenuButton(player, payload.button());
                                menu.broadcastChanges();
                            }
                        }));
    }

    public static void clickButton(int containerId, int button) {
        PacketDistributor.sendToServer(new MenuButton(containerId, button));
    }

    public static void sendSlot(ServerPlayer player, AbstractContainerMenu menu, int slot, ItemStack stack) {
        PacketDistributor.sendToPlayer(player,
                new LargeSlot(menu.containerId, menu.incrementStateId(), slot, FactoryStackHandler.saveStack(stack, player.serverLevel().registryAccess())));
    }

    public static void sendTradeOffers(ServerPlayer player, AbstractContainerMenu menu, MerchantOffers offers) {
        PacketDistributor.sendToPlayer(player, new TradeOffers(menu.containerId, offers.copy()));
    }

    public record LargeSlot(int containerId, int stateId, int slot, CompoundTag stack) implements CustomPacketPayload {
        public static final Type<LargeSlot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mekv.MODID, "large_slot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, LargeSlot> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.containerId);
                    buf.writeVarInt(value.stateId);
                    buf.writeVarInt(value.slot);
                    buf.writeNbt(value.stack);
                },
                buf -> new LargeSlot(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readNbt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record MenuButton(int containerId, int button) implements CustomPacketPayload {
        public static final Type<MenuButton> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mekv.MODID, "menu_button"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MenuButton> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.containerId);
                    buf.writeVarInt(value.button);
                },
                buf -> new MenuButton(buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record TradeOffers(int containerId, MerchantOffers offers) implements CustomPacketPayload {
        public static final Type<TradeOffers> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Mekv.MODID, "trade_offers"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TradeOffers> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> {
                    buf.writeVarInt(value.containerId);
                    MerchantOffers.STREAM_CODEC.encode(buf, value.offers);
                },
                buf -> new TradeOffers(buf.readVarInt(), MerchantOffers.STREAM_CODEC.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

