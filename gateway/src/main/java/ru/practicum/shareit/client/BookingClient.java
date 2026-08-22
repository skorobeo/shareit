package ru.practicum.shareit.client;
import ru.practicum.shareit.Constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingCreateDto;

@Service
public class BookingClient extends BaseClient {
    public BookingClient(@Value("${shareit-server.url}") String serverUrl,
                         RestTemplateBuilder builder) {
        super(serverUrl, builder);
    }

    private HttpHeaders headersWithUserId(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Constants.USER_ID_HEADER, String.valueOf(userId));
        return headers;
    }

    public ResponseEntity<String> createBooking(long userId, BookingCreateDto bookingCreateDto) {
        return post("/bookings", headersWithUserId(userId), bookingCreateDto);
    }

    public ResponseEntity<String> approveBooking(long userId, long bookingId, boolean approved) {
        return patch("/bookings/" + bookingId + "?approved=" + approved, headersWithUserId(userId), null);
    }

    public ResponseEntity<String> getBooking(long userId, long bookingId) {
        return get("/bookings/" + bookingId, headersWithUserId(userId));
    }

    public ResponseEntity<String> getBookingsByUser(long userId, String state) {
        return get("/bookings?state=" + state, headersWithUserId(userId));
    }

    public ResponseEntity<String> getBookingsByOwner(long userId, String state) {
        return get("/bookings/owner?state=" + state, headersWithUserId(userId));
    }
}