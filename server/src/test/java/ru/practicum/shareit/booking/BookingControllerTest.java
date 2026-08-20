package ru.practicum.shareit.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;
import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.State;
import ru.practicum.shareit.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingDto bookingDto() {
        return new BookingDto(1L,
                LocalDateTime.of(2025, 1, 1, 10, 0),
                LocalDateTime.of(2025, 1, 2, 10, 0),
                BookingStatus.WAITING,
                new BookingItemDto(5L, "Дрель"),
                new BookerDto(2L, "booker"));
    }

    @Test
    void createShouldReturn200() throws Exception {
        when(bookingService.create(eq(2L), any(BookingCreateDto.class)))
                .thenReturn(bookingDto());

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"start\":\"2025-01-01T10:00:00\","
                                + "\"end\":\"2025-01-02T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService).create(eq(2L), any(BookingCreateDto.class));
    }

    @Test
    void approveShouldReturn200() throws Exception {
        when(bookingService.approve(eq(1L), eq(1L), eq(true)))
                .thenReturn(bookingDto());

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", "1")
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(bookingService).approve(eq(1L), eq(1L), eq(true));
    }

    @Test
    void findByIdShouldReturn200() throws Exception {
        when(bookingService.findById(2L, 1L)).thenReturn(bookingDto());

        mockMvc.perform(get("/bookings/1")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item.name").value("Дрель"));

        verify(bookingService).findById(2L, 1L);
    }

    @Test
    void findByIdNotFoundShouldReturn404() throws Exception {
        when(bookingService.findById(2L, 99L))
                .thenThrow(new NotFoundException("Бронирование не найдено"));

        mockMvc.perform(get("/bookings/99")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllByBookerShouldReturn200() throws Exception {
        when(bookingService.findAllByBooker(2L, State.ALL)).thenReturn(List.of(bookingDto()));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService).findAllByBooker(2L, State.ALL);
    }

    @Test
    void findAllByOwnerShouldReturn200() throws Exception {
        when(bookingService.findAllByOwner(1L, State.WAITING)).thenReturn(List.of(bookingDto()));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", "1")
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(bookingService).findAllByOwner(1L, State.WAITING);
    }

    @Test
    void invalidStateShouldReturn400() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", "2")
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}