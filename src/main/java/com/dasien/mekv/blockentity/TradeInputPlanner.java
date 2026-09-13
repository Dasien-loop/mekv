package com.dasien.mekv.blockentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Plans one item group without mutating inventory or running reservations. */
final class TradeInputPlanner {
    private TradeInputPlanner() {
    }

    // Zero requirements mean incompatible lanes. Paused/occupied foreign lanes
    // are excluded by the caller. Null means preserve the original inventory.
    static int[] plan(int total, int[] requirements, int[] limits, boolean[] running) {
        int size = requirements.length;
        if (total < 0 || limits.length != size || running.length != size) {
            throw new IllegalArgumentException("Invalid distribution inputs");
        }
        int[] amounts = new int[size];
        List<Integer> candidates = new ArrayList<>();
        long remaining = total;
        for (int slot = 0; slot < size; slot++) {
            int required = requirements[slot];
            if (running[slot]) {
                if (required <= 0 || required > limits[slot]) {
                    return null;
                }
                amounts[slot] = required;
                remaining -= required;
            } else if (required > 0 && required <= limits[slot]) {
                candidates.add(slot);
            }
        }
        if (remaining < 0) {
            return null;
        }
        candidates.sort(Comparator.comparingInt((Integer slot) -> requirements[slot])
                .thenComparingInt(Integer::intValue));
        for (int slot : candidates) {
            if (requirements[slot] <= remaining) {
                amounts[slot] = requirements[slot];
                remaining -= requirements[slot];
            }
        }

        long capacity = 0;
        int high = 0;
        for (int slot = 0; slot < size; slot++) {
            if (amounts[slot] > 0) {
                capacity += limits[slot] - amounts[slot];
                high = Math.max(high, limits[slot]);
            }
        }
        if (capacity < remaining) {
            return null;
        }
        int low = 0;
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            long needed = 0;
            for (int slot = 0; slot < size; slot++) {
                if (amounts[slot] > 0) {
                    needed += Math.max(0, Math.min(middle, limits[slot]) - amounts[slot]);
                }
            }
            if (needed <= remaining) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        for (int slot = 0; slot < size; slot++) {
            if (amounts[slot] > 0) {
                int added = Math.max(0, Math.min(low, limits[slot]) - amounts[slot]);
                amounts[slot] += added;
                remaining -= added;
            }
        }
        for (int slot = 0; slot < size && remaining > 0; slot++) {
            if (amounts[slot] > 0 && amounts[slot] == low && amounts[slot] < limits[slot]) {
                amounts[slot]++;
                remaining--;
            }
        }
        return remaining == 0 ? amounts : null;
    }
}
