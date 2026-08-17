package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.dto.BookerDto;

public class BookingMapper {

    public static BookingDto toBookingDto(Booking booking) {
        BookingItemDto item = new BookingItemDto(
                booking.getItem().getId(),
                booking.getItem().getName());
        BookerDto booker = new BookerDto(
                booking.getBooker().getId(),
                booking.getBooker().getName());
        return new BookingDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus(),
                item,
                booker);
    }

    public static Booking toBooking(BookingCreateDto dto) {
        return new Booking(
                null,
                dto.getStart(),
                dto.getEnd(),
                null,
                null,
                null);
    }
}