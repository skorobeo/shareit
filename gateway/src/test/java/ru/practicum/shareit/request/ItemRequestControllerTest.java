package ru.practicum.shareit.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.client.RequestClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequestClient requestClient;

    @Test
    void createRequestShouldReturn200AndCallClient() throws Exception {
        when(requestClient.createRequest(eq(2L), any(ItemRequestCreateDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"description\":\"Нужна дрель\"}"));

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemRequestCreateDto("Нужна дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(requestClient).createRequest(eq(2L), any(ItemRequestCreateDto.class));
    }

    @Test
    void createRequestWithBlankDescriptionShouldReturn400() throws Exception {
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", "2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getOwnRequestsShouldReturn200AndCallClient() throws Exception {
        when(requestClient.getOwnRequests(2L)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(requestClient).getOwnRequests(2L);
    }

    @Test
    void getAllRequestsShouldReturn200AndCallClient() throws Exception {
        when(requestClient.getAllRequests(1L)).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(requestClient).getAllRequests(1L);
    }

    @Test
    void getRequestShouldReturn200AndCallClient() throws Exception {
        when(requestClient.getRequest(2L, 1L))
                .thenReturn(ResponseEntity.ok("{\"id\":1}"));

        mockMvc.perform(get("/requests/1")
                        .header("X-Sharer-User-Id", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(requestClient).getRequest(2L, 1L);
    }
}