package com.dasien.mekv.client.screen;

import static com.dasien.mekv.client.screen.TradeListLayout.*;

/** Dependency-free regression tests; also run by Gradle's check task. */
public final class TradeListLayoutTest {
    private static int checks;

    public static void main(String[] args) {
        int barXShift = LIST_WIDTH - 6;
        equal(158, contentWidth(barXShift), "content extends to the scrollbar, not just the left edge");
        for (int first : new int[]{0, 1, 5, 7}) {
            for (int row = 0; row < VISIBLE_ROWS; row++) {
                for (int x = 1; x < barXShift - 1; x++) {
                    for (int y = rowY(row); y < rowY(row + 1); y++) {
                        equal(first + row, offerAt(x + 0.5, y + 0.5, barXShift, first, 12),
                                "each painted pixel selects exactly its visible offer");
                    }
                }
                equal(first + row, offerAt(90, rowY(row), barXShift, first, 12),
                        "row boundary belongs to the following row");
                for (int part = 0; part < 3; part++) {
                    Bounds bounds = itemBounds(part);
                    for (int x = bounds.x(); x < bounds.x() + bounds.width(); x++) {
                        for (int y = bounds.y(); y < bounds.y() + bounds.height(); y++) {
                            equal(part, itemAt(x, y), "the entire slot has the correct item tooltip");
                            equal(first + row, offerAt(x + 1, rowY(row) + y, barXShift, first, 12),
                                    "item tooltip and click use the same offer");
                        }
                    }
                }
            }
        }
        for (int x : new int[]{-1, 0, barXShift - 1, barXShift, LIST_WIDTH - 1, LIST_WIDTH}) {
            equal(-1, offerAt(x, 20, barXShift, 0, 12), "border/scrollbar never selects an offer");
        }
        for (double y : new double[]{-1, 0, LIST_HEIGHT - 1, LIST_HEIGHT}) {
            equal(-1, offerAt(80, y, barXShift, 0, 12), "vertical border excluded");
        }
        equal(-1, offerAt(80, rowY(2), barXShift, 0, 2), "empty rows do not select hidden trades");
        equal(-1, offerAt(80, rowY(2), barXShift, 10, 12), "blank rows after scroll are excluded");
        equal(-1, offerAt(80, 20, barXShift, 0, 0), "empty offers");
        equal(-1, offerAt(80, 20, barXShift, 7, 2), "offers shrink while the list is scrolled");
        equal(-1, itemAt(55, 18), "arrow has no item tooltip");
        equal(-1, itemAt(26, 18), "plus has no item tooltip");
        equal(-1, itemAt(104, 20), "stock label has no item tooltip");
        equal(-1, itemAt(8, 30), "slot bottom is excluded");
        for (int scale : new int[]{1, 2, 3, 4}) {
            for (int left : new int[]{-30, 0, 47, 130}) {
                for (int top : new int[]{0, 27, 89}) {
                    double screenX = (left + 90.5) * scale;
                    double screenY = (top + rowY(3) + 20.5) * scale;
                    equal(8, offerAt(screenX / scale - left, screenY / scale - top, barXShift, 5, 12),
                            "dragged/resized/scaled window retains item/offer alignment");
                }
            }
        }
        System.out.println("Trade UI layout: " + checks + " checks passed.");
    }

    private static void equal(int expected, int actual, String context) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(context + ": expected " + expected + ", got " + actual);
        }
    }
}
