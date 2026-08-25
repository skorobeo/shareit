package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

class DtoNoArgConstructorTest {

    @Test
    void allDtosShouldHaveUsableNoArgConstructors() {
        assertThat(new UserDto()).isNotNull();
        assertThat(new ItemDto()).isNotNull();
        assertThat(new ItemViewDto()).isNotNull();
        assertThat(new CommentDto()).isNotNull();
        assertThat(new ItemRequestDto()).isNotNull();
        assertThat(new ItemRequestCreateDto()).isNotNull();
        assertThat(new BookingDto()).isNotNull();
        assertThat(new BookingCreateDto()).isNotNull();
        assertThat(new BookingItemDto()).isNotNull();
        assertThat(new BookerDto()).isNotNull();
        assertThat(new BookingShortDto()).isNotNull();
    }
}