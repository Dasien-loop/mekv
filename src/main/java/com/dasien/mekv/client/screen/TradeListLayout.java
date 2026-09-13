package com.dasien.mekv.client.screen;

/** Shared half-open bounds for trade rendering, clicking and item tooltips. */
final class TradeListLayout {
    static final int ROW_HEIGHT = 32;
    static final int VISIBLE_ROWS = 5;
    static final int LIST_WIDTH = 166;
    static final int LIST_HEIGHT = VISIBLE_ROWS * ROW_HEIGHT + 2;
    private static final Bounds[] ITEMS = {
            new Bounds(6, 12, 18, 18),
            new Bounds(32, 12, 18, 18),
            new Bounds(80, 12, 18, 18)
    };

    private TradeListLayout() {
    }

    static int contentWidth(int barXShift) {
        return barXShift - 2;
    }

    static int rowY(int row) {
        return 1 + row * ROW_HEIGHT;
    }

    static int offerAt(double x, double y, int barXShift, int first, int offerCount) {
        int rows = Math.min(VISIBLE_ROWS, Math.max(0, offerCount - first));
        if (first < 0 || x < 1 || x >= barXShift - 1 || y < 1 || y >= rowY(rows)) {
            return -1;
        }
        return first + (int) ((y - 1) / ROW_HEIGHT);
    }

    static Bounds itemBounds(int part) {
        return ITEMS[part];
    }

    static int itemAt(double x, double y) {
        for (int part = 0; part < ITEMS.length; part++) {
            if (ITEMS[part].contains(x, y)) {
                return part;
            }
        }
        return -1;
    }

    record Bounds(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
