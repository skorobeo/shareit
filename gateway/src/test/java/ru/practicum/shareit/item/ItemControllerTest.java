package ru.practicum.shareit.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.client.ItemClient;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

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

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemClient itemClient;

    @Test
    void createItemShouldReturn200AndCallClient() throws Exception {
        when(itemClient.createItem(eq(1L), any(ItemDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"requestId\":7}"));

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Мощная", true, null, 7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(7));

        verify(itemClient).createItem(eq(1L), any(ItemDto.class));
    }

    @Test
    void createItemWithBlankNameShouldReturn400() throws Exception {
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"description\":\"desc\",\"available\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createItemWithoutAvailableShouldReturn400() throws Exception {
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"desc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateItemShouldReturn200AndCallClient() throws Exception {
        when(itemClient.updateItem(eq(1L), eq(5L), any(ItemDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":5,\"name\":\"Шуруповерт\"}"));

        mockMvc.perform(patch("/items/5")
                        .header("X-Sharer-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Шуруповерт", null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Шуруповерт"));

        verify(itemClient).updateItem(eq(1L), eq(5L), any(ItemDto.class));
    }

    @Test
    void getItemShouldReturn200AndCallClient() throws Exception {
        when(itemClient.getItem(5L, 1L))
                .thenReturn(ResponseEntity.ok("{\"id\":5,\"name\":\"Дрель\"}"));

        mockMvc.perform(get("/items/5")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Дрель"));

        verify(itemClient).getItem(5L, 1L);
    }

    @Test
    void getItemsByUserShouldReturn200AndCallClient() throws Exception {
        when(itemClient.getItemsByUser(1L)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(itemClient).getItemsByUser(1L);
    }

    @Test
    void searchItemsShouldForwardHeaderAndCallClient() throws Exception {
        when(itemClient.searchItems(1L, "дрель"))
                .thenReturn(ResponseEntity.ok("[{\"id\":5}]"));

        mockMvc.perform(get("/items/search")
                        .header("X-Sharer-User-Id", "1")
                        .param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5));

        verify(itemClient).searchItems(1L, "дрель");
    }

    @Test
    void addCommentShouldReturn200AndCallClient() throws Exception {
        when(itemClient.addComment(eq(2L), eq(5L), any(CommentDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"text\":\"Отличная вещь\"}"));

        mockMvc.perform(post("/items/5/comment")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Отличная вещь", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Отличная вещь"));

        verify(itemClient).addComment(eq(2L), eq(5L), any(CommentDto.class));
    }

    @Test
    void addCommentWithBlankTextShouldReturn400() throws Exception {
        mockMvc.perform(post("/items/5/comment")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
    }
}