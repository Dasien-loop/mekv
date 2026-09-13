package com.dasien.mekv.util;

import com.dasien.mekv.menu.TraderFactoryMenu;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

public final class FactoryRegressionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        int[] costs = filled(9, 4);
        int[] limits = filled(9, 64);
        int[] result = TradeInputPlanner.plan(27, costs, limits, new boolean[9]);
        equal(6, Arrays.stream(result).map(n -> n / 4).sum(), "nine three-iron stacks become six trades");
        equal(1, (int) Arrays.stream(result).filter(n -> n % 4 != 0).count(), "only one remainder lane");
        equal(0, drain(24, costs, limits), "old stranded inputs drain completely");
        equal(3, drain(27, costs, limits), "only genuine insufficient remainder remains");
        int[] reservedCosts = {4, 8, 8};
        int[] reserved = TradeInputPlanner.plan(12, reservedCosts, filled(3, 64), new boolean[]{true, false, false});
        require(reserved[0] >= 4, "running lane retains captured price after price change");
        require(TradeInputPlanner.plan(3, costs, limits, new boolean[]{true, false, false, false, false, false, false, false, false}) == null,
                "unfunded reservation fails without mutation");
        require(TradeInputPlanner.plan(577, costs, limits, new boolean[9]) == null, "capacity failure preserves original plan");
        int[] excluded = TradeInputPlanner.plan(12, new int[]{4, 0, 4}, filled(3, 64), new boolean[3]);
        equal(0, excluded[1], "paused or incompatible lane receives no input");
        int[] mixed = TradeInputPlanner.plan(31, new int[]{4, 5, 8}, new int[]{16, 15, 32}, new boolean[]{true, false, false});
        equal(31, Arrays.stream(mixed).sum(), "mixed prices preserve input");
        require(mixed[0] >= 4, "mixed prices preserve running reservation");
        for (boolean pause : new boolean[]{false, true}) {
            simulateBatch(pause, 64);
            simulateBatch(pause, 4096);
        }
        Random random = new Random(731);
        for (int i = 0; i < 5000; i++) {
            int count = 1 + random.nextInt(19);
            int price = 1 + random.nextInt(64);
            int total = random.nextInt(count * 64 + 1);
            int[] plan = TradeInputPlanner.plan(total, filled(count, price), filled(count, 64), new boolean[count]);
            if (plan != null) {
                equal(total, Arrays.stream(plan).sum(), "conservation");
                for (int amount : plan) {
                    require(amount >= 0 && amount <= 64, "slot capacity");
                }
            }
        }
        for (int x : new int[]{TraderFactoryMenu.VILLAGER_X, TraderFactoryMenu.WORKSTATION_X,
                TraderFactoryMenu.TRADE_STATUS_X, TraderFactoryMenu.GLOBAL_CONTROL_X,
                TraderFactoryMenu.COST_A_X, TraderFactoryMenu.COST_B_X, TraderFactoryMenu.RESULT_X}) {
            equal(0, (x - 27) % 19, "ultimate header aligns to lane grid");
        }
        Path assets = Path.of("src/main/resources/assets/mekv");
        int models = 0;
        for (String tier : new String[]{"basic", "advanced", "elite", "ultimate", "absolute", "supreme", "cosmic", "infinite"}) {
            for (String type : new String[]{"farmer", "iron_golem", "trader"}) {
                String name = tier + "_" + type + "_factory";
                var item = JsonParser.parseString(Files.readString(assets.resolve("models/item/" + name + ".json"))).getAsJsonObject();
                equal(225, item.getAsJsonObject("display").getAsJsonObject("gui").getAsJsonArray("rotation").get(1).getAsInt(), "front-facing GUI model");
                for (String suffix : new String[]{"", "_active"}) {
                    var model = JsonParser.parseString(Files.readString(assets.resolve("models/block/" + name + suffix + ".json"))).getAsJsonObject();
                    for (var entry : model.getAsJsonObject("textures").entrySet()) {
                        String texture = entry.getValue().getAsString();
                        if (texture.startsWith("mekv:")) {
                            require(Files.isRegularFile(assets.resolve("textures/" + texture.substring(5) + ".png")), texture);
                        }
                    }
                }
                models++;
            }
        }
        equal(24, models, "all factory items checked");
        require(Files.readString(Path.of("tools/generate-extra-factory-resources.ps1"))
                .contains("rotation = @(30, 225, 0)"), "resource generator retains front-facing GUI angle");
        System.out.println("Factory regressions: " + checks + " checks passed.");
    }

    private static int drain(int total, int[] costs, int[] limits) {
        for (int iteration = 0; iteration < 100; iteration++) {
            int[] plan = TradeInputPlanner.plan(total, costs, limits, new boolean[costs.length]);
            require(plan != null, "drain plan");
            int consumed = 0;
            for (int lane = 0; lane < costs.length; lane++) {
                if (plan[lane] >= costs[lane]) {
                    consumed += costs[lane];
                }
            }
            total -= consumed;
            if (consumed == 0) {
                return total;
            }
        }
        throw new AssertionError("Failed to drain");
    }

    /** Count-level simulation using the production planner with asynchronous completion. */
    private static void simulateBatch(boolean pause, int limit) {
        int[] input = new int[9];
        int[] progress = new int[9];
        int pending = 1000;
        int completed = 0;
        for (int tick = 0; tick < 4000; tick++) {
            boolean paused = pause && tick >= 9 && tick < 57;
            for (int lane = 0; lane < 9; lane++) {
                int added = Math.min(pending, Math.min(64, limit - input[lane]));
                input[lane] += added;
                pending -= added;
            }
            int[] costs = filled(9, 4);
            boolean[] running = new boolean[9];
            int total = Arrays.stream(input).sum();
            for (int lane = 0; lane < 9; lane++) {
                running[lane] = progress[lane] > 0;
            }
            int held = paused ? input[7] : 0;
            if (paused) {
                costs[7] = 0;
                running[7] = false;
                total -= held;
            }
            int[] plan = TradeInputPlanner.plan(total, costs, filled(9, limit), running);
            require(plan != null, "batch distribution succeeds");
            if (paused) {
                plan[7] = held;
            }
            input = plan;
            for (int lane = 0; lane < 9; lane++) {
                if (paused && lane == 7) {
                    continue;
                }
                if (progress[lane] > 0) {
                    require(input[lane] >= 4, "running reservation preserved");
                    if (++progress[lane] >= 3 + lane % 5) {
                        input[lane] -= 4;
                        completed++;
                        progress[lane] = 0;
                    }
                } else if (input[lane] >= 4) {
                    progress[lane] = 1;
                }
            }
            equal(1000, pending + Arrays.stream(input).sum() + completed * 4, "batch conservation");
            if (completed == 250) {
                equal(0, Arrays.stream(input).sum(), "batch leaves no iron");
                return;
            }
        }
        throw new AssertionError("1000 iron batch stalled after " + completed + " trades");
    }

    private static int[] filled(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    private static void equal(int expected, int actual, String message) {
        require(expected == actual, message + ": expected " + expected + ", actual " + actual);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
