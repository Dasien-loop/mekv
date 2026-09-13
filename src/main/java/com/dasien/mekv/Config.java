package com.dasien.mekv;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Mekv.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue ENERGY_PER_TRADE = BUILDER
            .comment("FE consumed for each completed villager trade")
            .defineInRange("energyPerTrade", 200, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENERGY_PER_GOLEM = BUILDER
            .comment("FE consumed when an iron golem is killed inside a factory")
            .defineInRange("energyPerGolem", 1000, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENERGY_PER_CROP_AGE = BUILDER
            .comment("FE consumed each time a crop ages inside a farmer factory")
            .defineInRange("energyPerCropAge", 50, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue ENERGY_PER_HARVEST = BUILDER
            .comment("FE consumed when a mature crop is harvested")
            .defineInRange("energyPerHarvest", 200, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue TRADE_DURATION_TICKS = BUILDER
            .comment("Ticks required to complete one trader factory process (10-20)")
            .defineInRange("tradeDurationTicks", 15, 10, 20);

    private static final ForgeConfigSpec.DoubleValue FACTORY_ENERGY_CAPACITY_MULTIPLIER = BUILDER
            .comment("Multiplier applied to the configured factory energy capacity")
            .defineInRange("factoryEnergyCapacityMultiplier", 1.0D, 0.0D, 1000.0D);

    private static final ForgeConfigSpec.DoubleValue FACTORY_ENERGY_TRANSFER_MULTIPLIER = BUILDER
            .comment("Multiplier applied to the configured factory energy transfer rate")
            .defineInRange("factoryEnergyTransferMultiplier", 1.0D, 0.0D, 1000.0D);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int energyPerTrade;
    public static int energyPerGolem;
    public static int energyPerCropAge;
    public static int energyPerHarvest;
    public static int tradeDurationTicks = 15;
    public static double factoryEnergyCapacityMultiplier = 1.0D;
    public static double factoryEnergyTransferMultiplier = 1.0D;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        energyPerTrade = ENERGY_PER_TRADE.get();
        energyPerGolem = ENERGY_PER_GOLEM.get();
        energyPerCropAge = ENERGY_PER_CROP_AGE.get();
        energyPerHarvest = ENERGY_PER_HARVEST.get();
        tradeDurationTicks = TRADE_DURATION_TICKS.get();
        factoryEnergyCapacityMultiplier = FACTORY_ENERGY_CAPACITY_MULTIPLIER.get();
        factoryEnergyTransferMultiplier = FACTORY_ENERGY_TRANSFER_MULTIPLIER.get();
    }
}
