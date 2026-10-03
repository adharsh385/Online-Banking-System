package com.banking.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilsTest {

    @Test
    void returnsEmptyStringForNullDate() {
        assertEquals("", DateUtils.formatDateTime(null));
    }

    @Test
    void formatsDateUsingDisplayPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2025, 3, 4, 15, 5);
        String expected = dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a"));

        assertEquals(expected, DateUtils.formatDateTime(dateTime));
    }
}
