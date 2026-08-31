package com.icsfilter;

import com.icsfilter.model.CalendarSource;
import com.icsfilter.ui.UiPalette;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UiPaletteTest {

    @Test
    void resolvesEditedSourceColorByCurrentName() {
        // A source was loaded and its events still reference this pre-edit object.
        CalendarSource stale = new CalendarSource("Arbeit", "https://example.com/arbeit.ics");
        // The user edits the source: same name, new colour. The list now holds this one.
        CalendarSource edited = new CalendarSource("Arbeit", "https://example.com/arbeit.ics", "", "#123456");
        List<CalendarSource> sourceOrder = List.of(edited);

        // The view must re-resolve against the current source object by name.
        CalendarSource current = UiPalette.currentSource(stale, sourceOrder);
        assertSame(edited, current);

        Color color = UiPalette.resolveColor(current, sourceOrder.indexOf(current));
        assertEquals(Color.web("#123456"), color);
    }

    @Test
    void fallsBackToPassedSourceWhenNoNameMatch() {
        CalendarSource source = new CalendarSource("Arbeit", "https://example.com/arbeit.ics");
        CalendarSource other = new CalendarSource("Klasse", "https://example.com/klasse.ics", "", "#abcdef");
        assertEquals(source, UiPalette.currentSource(source, List.of(other)));
    }
}
