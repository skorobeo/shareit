package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    private ItemRequestDto requestDto() {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(1L);
        dto.setDescription("Нужна дрель");
        dto.setRequestorId(2L);
        dto.setItems(List.of());
        dto.setCreated(LocalDateTime.of(2025, 1, 1, 12, 0, 0));
        return dto;
    }

    @Test
    void createShouldReturn200() throws Exception {
        when(itemRequestService.create(eq(2L), any(ItemRequestCreateDto.class)))
                .thenReturn(requestDto());

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemRequestCreateDto("Нужна дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Нужна дрель"));

        verify(itemRequestService).create(eq(2L), any(ItemRequestCreateDto.class));
    }

    @Test
    void createWithUnknownUserShouldReturn404() throws Exception {
        when(itemRequestService.create(eq(99L), any(ItemRequestCreateDto.class)))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemRequestCreateDto("Нужна дрель"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllByRequestorShouldReturn200() throws Exception {
        when(itemRequestService.findAllByRequestor(2L)).thenReturn(List.of(requestDto()));

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemRequestService).findAllByRequestor(2L);
    }

    @Test
    void findAllOthersShouldReturn200() throws Exception {
        when(itemRequestService.findAllOthers(1L)).thenReturn(List.of(requestDto()));

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemRequestService).findAllOthers(1L);
    }

    @Test
    void findByIdShouldReturn200() throws Exception {
        when(itemRequestService.findById(2L, 1L)).thenReturn(requestDto());

        mockMvc.perform(get("/requests/1")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(itemRequestService).findById(2L, 1L);
    }

    @Test
    void findByIdNotFoundShouldReturn404() throws Exception {
        when(itemRequestService.findById(2L, 99L))
                .thenThrow(new NotFoundException("Запрос не найден"));

        mockMvc.perform(get("/requests/99")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isNotFound());
    }
}