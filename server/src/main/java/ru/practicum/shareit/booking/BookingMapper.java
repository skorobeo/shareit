package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.*;

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

    public static BookingShortDto toShortDto(Booking booking) {
        return new BookingShortDto(
                booking.getId(),
                booking.getBooker().getId());
    }
}