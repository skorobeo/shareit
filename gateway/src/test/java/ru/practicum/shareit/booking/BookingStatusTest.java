package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStatusTest {

    @Test
    void shouldExposeAllStatuses() {
        assertThat(BookingStatus.values())
                .containsExactly(BookingStatus.WAITING, BookingStatus.APPROVED,
                        BookingStatus.REJECTED, BookingStatus.CANCELED);
    }
}