package com.dasien.mekv.client;

import com.dasien.mekv.inventory.FactoryStackHandler;
import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.network.FactoryNetwork;
import net.minecraft.client.Minecraft;

public final class FactoryPacketHandler {
    private FactoryPacketHandler() {
    }

    public static void handle(FactoryNetwork.LargeSlot packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof FactoryMenu menu
                && menu.containerId == packet.containerId() && packet.stack() != null
                && packet.slot() >= 0 && packet.slot() < menu.slots.size()) {
            menu.setItem(packet.slot(), packet.stateId(), FactoryStackHandler.loadStack(packet.stack()));
        }
    }
}
