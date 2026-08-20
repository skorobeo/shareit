package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.State;

import java.util.List;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    private static final String UserIdHeader = "X-Sharer-User-Id";

    @PostMapping
    public BookingDto create(@RequestHeader(UserIdHeader) Long userId,
                             @Valid @RequestBody BookingCreateDto dto) {
        return bookingService.create(userId, dto);
    }

    @PatchMapping("/{bookingId}")
    public BookingDto approve(@RequestHeader(UserIdHeader) Long ownerId,
                              @PathVariable Long bookingId,
                              @RequestParam Boolean approved) {
        return bookingService.approve(ownerId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public BookingDto findById(@RequestHeader(UserIdHeader) Long userId,
                               @PathVariable Long bookingId) {
        return bookingService.findById(userId, bookingId);
    }

    @GetMapping
    public List<BookingDto> findAllByBooker(@RequestHeader(UserIdHeader) Long userId,
                                            @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.findAllByBooker(userId, State.from(state));
    }

    @GetMapping("/owner")
    public List<BookingDto> findAllByOwner(@RequestHeader(UserIdHeader) Long ownerId,
                                           @RequestParam(defaultValue = "ALL") String state) {
        return bookingService.findAllByOwner(ownerId, State.from(state));
    }
}