package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.client.BookingClient;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingClient bookingClient;

    @Test
    void createBookingShouldReturn200AndCallClient() throws Exception {
        when(bookingClient.createBooking(eq(2L), any(BookingCreateDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"WAITING\"}"));

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = LocalDateTime.now().plusDays(2).withNano(0);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"start\":\"" + start + "\","
                                + "\"end\":\"" + end + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingClient).createBooking(eq(2L), any(BookingCreateDto.class));
    }

    @Test
    void createBookingWithoutStartShouldReturn400() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"end\":\"2025-01-02T10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBookingWithEndBeforeStartShouldReturn400() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = LocalDateTime.now().plusDays(1).withNano(0);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"start\":\"" + start + "\","
                                + "\"end\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBookingWithPastStartShouldReturn400() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(1).withNano(0);
        LocalDateTime end = LocalDateTime.now().plusDays(1).withNano(0);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"start\":\"" + start + "\","
                                + "\"end\":\"" + end + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approveBookingShouldReturn200AndCallClient() throws Exception {
        when(bookingClient.approveBooking(1L, 1L, true))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"APPROVED\"}"));

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", "1")
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingClient).approveBooking(1L, 1L, true);
    }

    @Test
    void getBookingShouldReturn200AndCallClient() throws Exception {
        when(bookingClient.getBooking(2L, 1L))
                .thenReturn(ResponseEntity.ok("{\"id\":1}"));

        mockMvc.perform(get("/bookings/1")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingClient).getBooking(2L, 1L);
    }

    @Test
    void getBookingsByUserShouldReturn200AndCallClient() throws Exception {
        when(bookingClient.getBookingsByUser(2L, "ALL"))
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(bookingClient).getBookingsByUser(2L, "ALL");
    }

    @Test
    void getBookingsByOwnerShouldReturn200AndCallClient() throws Exception {
        when(bookingClient.getBookingsByOwner(1L, "WAITING"))
                .thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", "1")
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(bookingClient).getBookingsByOwner(1L, "WAITING");
    }

    @Test
    void invalidStateShouldReturn400AndNotCallClient() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}