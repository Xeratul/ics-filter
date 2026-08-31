package com.icsfilter.ui;

import com.icsfilter.model.CalendarSource;
import javafx.scene.paint.Color;

import java.util.List;

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

    /** The source's own color when set, otherwise the palette color by index. */
    public static Color resolveColor(CalendarSource source, int index) {
        if (source != null && !source.color().isBlank()) {
            try {
                return Color.web(source.color());
            } catch (IllegalArgumentException ignored) {
                // Fall through to the default palette color.
            }
        }
        return colorFor(index);
    }

    /** Returns the currently-configured source with the same name as {@code source},
     *  or {@code source} itself when no match is found. Views use this so that
     *  editing a source's name/colour takes effect even though already-loaded
     *  events still reference the pre-edit {@link CalendarSource} object. */
    public static CalendarSource currentSource(CalendarSource source, List<CalendarSource> sourceOrder) {
        if (source == null) {
            return null;
        }
        if (sourceOrder != null) {
            for (CalendarSource s : sourceOrder) {
                if (s.name().equals(source.name())) {
                    return s;
                }
            }
        }
        return source;
    }

    /** Converts a color to a CSS hex string, e.g. {@code "#e63946"}. */
    public static String toCss(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }
}
