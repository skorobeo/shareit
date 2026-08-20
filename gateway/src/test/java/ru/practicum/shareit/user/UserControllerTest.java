package ru.practicum.shareit.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.client.UserClient;
import ru.practicum.shareit.user.dto.UserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserClient userClient;

    @Test
    void createUserShouldReturn200AndCallClient() throws Exception {
        when(userClient.createUser(any(UserDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Ivan\"}"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Ivan", "ivan@mail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(userClient).createUser(any(UserDto.class));
    }

    @Test
    void createUserWithBlankEmailShouldReturn400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\",\"email\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserWithBadEmailFormatShouldReturn400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ivan\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserShouldReturn200AndCallClient() throws Exception {
        when(userClient.updateUser(eq(1L), any(UserDto.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Petr\"}"));

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Petr", "petr@mail.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Petr"));

        verify(userClient).updateUser(eq(1L), any(UserDto.class));
    }

    @Test
    void getUserShouldReturn200AndCallClient() throws Exception {
        when(userClient.getUser(1L))
                .thenReturn(ResponseEntity.ok("{\"id\":1,\"name\":\"Ivan\"}"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":1}"));

        verify(userClient).getUser(1L);
    }

    @Test
    void getAllUsersShouldReturn200AndCallClient() throws Exception {
        when(userClient.getAllUsers()).thenReturn(ResponseEntity.ok("[]"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userClient).getAllUsers();
    }

    @Test
    void deleteUserShouldReturn200AndCallClient() throws Exception {
        when(userClient.deleteUser(1L)).thenReturn(ResponseEntity.ok(""));

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isOk());

        verify(userClient).deleteUser(1L);
    }
}