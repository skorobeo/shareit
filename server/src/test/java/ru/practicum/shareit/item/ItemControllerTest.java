package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void createShouldReturn201() throws Exception {
        when(itemService.create(eq(1L), any(ItemDto.class)))
                .thenReturn(new ItemDto(1L, "Дрель", "Мощная", true, 1L, null));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Мощная", true, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerId").value(1));

        verify(itemService).create(eq(1L), any(ItemDto.class));
    }

    @Test
    void updateShouldReturn200() throws Exception {
        when(itemService.update(eq(1L), eq(5L), any(ItemDto.class)))
                .thenReturn(new ItemDto(5L, "Шуруповерт", "Описание", true, 1L, null));

        mockMvc.perform(patch("/items/5")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Шуруповерт", null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Шуруповерт"));

        verify(itemService).update(eq(1L), eq(5L), any(ItemDto.class));
    }

    @Test
    void findByIdShouldReturn200() throws Exception {
        when(itemService.findById(1L, 5L))
                .thenReturn(new ItemViewDto(5L, "Дрель", "Мощная", true, null,
                        null, null, List.of()));

        mockMvc.perform(get("/items/5")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Дрель"));

        verify(itemService).findById(1L, 5L);
    }

    @Test
    void findByIdNotFoundShouldReturn404() throws Exception {
        when(itemService.findById(1L, 99L))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(get("/items/99")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllByOwnerShouldReturn200() throws Exception {
        when(itemService.findAllByOwner(1L))
                .thenReturn(List.of(new ItemViewDto(5L, "Дрель", "Мощная", true, null,
                        null, null, List.of())));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemService).findAllByOwner(1L);
    }

    @Test
    void searchShouldReturn200() throws Exception {
        when(itemService.search("дрель"))
                .thenReturn(List.of(new ItemDto(5L, "Дрель", "Мощная", true, 1L, null)));

        mockMvc.perform(get("/items/search")
                        .header("X-Sharer-User-Id", "1")
                        .param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemService).search("дрель");
    }

    @Test
    void addCommentShouldReturn200() throws Exception {
        when(itemService.addComment(eq(2L), eq(5L), any(CommentDto.class)))
                .thenReturn(new CommentDto(1L, "Отличная вещь", "booker", null, 5L));

        mockMvc.perform(post("/items/5/comment")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Отличная вещь", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отличная вещь"));

        verify(itemService).addComment(eq(2L), eq(5L), any(CommentDto.class));
    }

    @Test
    void addCommentWithBlankTextShouldReturn400() throws Exception {
        when(itemService.addComment(anyLong(), anyLong(), any(CommentDto.class)))
                .thenThrow(new ValidationException("Текст отзыва не может быть пустым"));

        mockMvc.perform(post("/items/5/comment")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, " ", null, null))))
                .andExpect(status().isBadRequest());
    }
}