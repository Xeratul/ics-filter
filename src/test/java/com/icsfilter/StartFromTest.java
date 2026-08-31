package com.icsfilter;

import com.icsfilter.model.StartFrom;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartFromTest {

    @Test
    void yearUsesFirstDayOfYear() {
        assertEquals(LocalDate.of(2026, 1, 1),
                StartFrom.YEAR.effectiveDate(LocalDate.of(2026, 8, 31)));
    }

    @Test
    void monthUsesFirstDayOfMonth() {
        assertEquals(LocalDate.of(2026, 8, 1),
                StartFrom.MONTH.effectiveDate(LocalDate.of(2026, 8, 31)));
    }

    @Test
    void todayUsesReferenceDate() {
        assertEquals(LocalDate.of(2026, 8, 31),
                StartFrom.TODAY.effectiveDate(LocalDate.of(2026, 8, 31)));
    }

    @Test
    void nullReferenceFallsBackToToday() {
        assertEquals(LocalDate.now(), StartFrom.TODAY.effectiveDate(null));
    }
}
