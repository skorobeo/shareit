package ru.practicum.shareit.booking.dto;

import ru.practicum.shareit.exception.ValidationException;

import java.util.Locale;

public enum State {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static State from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        try {
            return State.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown state: " + value);
        }
    }
}