package com.dasien.mekv.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure count planner; item identity and paused-lane ownership stay with the factory. */
public final class TradeInputPlanner {
    private TradeInputPlanner() {
    }

    /** Zero cost excludes a lane. Returns null if the entire input cannot be preserved. */
    public static int[] plan(int total, int[] costs, int[] limits, boolean[] running) {
        if (total < 0 || costs.length != limits.length || costs.length != running.length) {
            throw new IllegalArgumentException("Invalid distribution inputs");
        }
        int[] amounts = new int[costs.length];
        List<Integer> targets = new ArrayList<>();
        List<Integer> candidates = new ArrayList<>();
        long remaining = total;
        for (int lane = 0; lane < costs.length; lane++) {
            if (costs[lane] <= 0 || costs[lane] > limits[lane]) {
                if (running[lane]) {
                    return null;
                }
                continue;
            }
            if (running[lane]) {
                targets.add(lane);
                amounts[lane] = costs[lane];
                remaining -= costs[lane];
            } else {
                candidates.add(lane);
            }
        }
        if (remaining < 0) {
            return null;
        }
        candidates.sort(Comparator.comparingInt((Integer lane) -> costs[lane]).thenComparingInt(lane -> lane));
        for (int lane : candidates) {
            if (costs[lane] <= remaining) {
                targets.add(lane);
                amounts[lane] = costs[lane];
                remaining -= costs[lane];
            }
        }
        if (targets.isEmpty()) {
            if (total == 0) {
                return amounts;
            }
            // Keep a final incomplete trade together, ready for the next insertion.
            for (int lane : candidates) {
                if (total <= limits[lane]) {
                    amounts[lane] = total;
                    return amounts;
                }
            }
            return null;
        }
        targets.sort(Integer::compareTo);
        // Level by complete operations, not individual items, without iterating
        // over every item in an Extras-sized stack. Reservations remain funded.
        long low = 1;
        long high = 1;
        for (int lane : targets) {
            high = Math.max(high, limits[lane] / costs[lane]);
        }
        while (low < high) {
            long middle = (low + high + 1) / 2;
            long needed = 0;
            for (int lane : targets) {
                needed += Math.min(middle, limits[lane] / costs[lane]) * costs[lane] - amounts[lane];
            }
            if (needed <= remaining) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        for (int lane : targets) {
            int count = (int) (Math.min(low, limits[lane] / costs[lane]) * costs[lane]);
            remaining -= count - amounts[lane];
            amounts[lane] = count;
        }
        for (int lane : targets) {
            if (remaining >= costs[lane] && limits[lane] - amounts[lane] >= costs[lane]) {
                amounts[lane] += costs[lane];
                remaining -= costs[lane];
            }
        }
        // Pack the remainder into as few lanes as capacity permits.
        for (int lane : targets) {
            int added = (int) Math.min(remaining, limits[lane] - amounts[lane]);
            amounts[lane] += added;
            remaining -= added;
        }
        return remaining == 0 ? amounts : null;
    }
}
