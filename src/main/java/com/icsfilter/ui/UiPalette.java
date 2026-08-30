package com.icsfilter.ui;

import javafx.scene.paint.Color;

/** Provides a stable color per source index. */
public final class UiPalette {

    private static final Color[] COLORS = {
            Color.web("#e63946"), Color.web("#457b9d"), Color.web("#2a9d8f"),
            Color.web("#f4a261"), Color.web("#9b5de5"), Color.web("#00b4d8"),
            Color.web("#f15bb5"), Color.web("#90be6d"), Color.web("#ff924c"),
            Color.web("#4cc9f0"), Color.web("#c77dff"), Color.web("#f9c74f")
    };

    private UiPalette() {
    }

    public static Color colorFor(int index) {
        return COLORS[Math.floorMod(index, COLORS.length)];
    }
}
