package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemRequestTests {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long createUser(String name) throws Exception {
        int n = SEQ.incrementAndGet();
        UserDto dto = new UserDto(null, name, "requser" + n + "@mail.com");
        String body = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createRequest(Long userId, String description) throws Exception {
        String body = mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("description", description))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("Создание запроса — должен вернуть 200")
    void createRequestShouldReturn200() throws Exception {
        Long userId = createUser("Requestor");

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("description", "Нужна дрель"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.requestorId").value(userId))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Создание запроса — должен вернуть 400 при пустом описании")
    void createRequestShouldReturn400WhenDescriptionBlank() throws Exception {
        Long userId = createUser("Requestor");

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("description", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание запроса — должен вернуть 404 при отсутствии пользователя")
    void createRequestShouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("description", "Нужна дрель"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список собственных запросов — должен вернуть 200")
    void findAllByRequestorShouldReturn200() throws Exception {
        Long userId = createUser("Requestor");
        createRequest(userId, "Нужна дрель");
        createRequest(userId, "Нужна пила");

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].description").value("Нужна пила"));
    }

    @Test
    @DisplayName("Список собственных запросов — должен вернуть 404 при отсутствии пользователя")
    void findAllByRequestorShouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список чужих запросов — должен вернуть только запросы других")
    void findAllOthersShouldReturnOthersOnly() throws Exception {
        Long requestor = createUser("Requestor");
        Long viewer = createUser("Viewer");
        String desc = "Нужна дрель " + SEQ.get();
        createRequest(requestor, desc);

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", viewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].description").value(org.hamcrest.Matchers.hasItem(desc)));
    }

    @Test
    @DisplayName("Список чужих запросов — должен исключить свои запросы")
    void findAllOthersShouldExcludeOwn() throws Exception {
        Long user = createUser("Requestor");
        String desc = "Свой запрос " + SEQ.get();
        createRequest(user, desc);

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].description")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(desc))));
    }

    @Test
    @DisplayName("Получение запроса — должен вернуть 200")
    void findByIdShouldReturn200() throws Exception {
        Long userId = createUser("Requestor");
        Long requestId = createRequest(userId, "Нужна дрель");

        mockMvc.perform(get("/requests/" + requestId)
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.description").value("Нужна дрель"));
    }

    @Test
    @DisplayName("Получение запроса — должен вернуть 404 для несуществующего запроса")
    void findByIdShouldReturn404WhenNotFound() throws Exception {
        Long userId = createUser("Requestor");

        mockMvc.perform(get("/requests/999999")
                        .header("X-Sharer-User-Id", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Запрос — должен содержать вещи, добавленные по нему")
    void requestShouldContainLinkedItems() throws Exception {
        Long requestor = createUser("Requestor");
        Long owner = createUser("Owner");
        Long requestId = createRequest(requestor, "Нужна дрель");

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Дрель", "Мощная дрель", true, null, requestId))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/requests/" + requestId)
                        .header("X-Sharer-User-Id", requestor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Дрель"));
    }
}