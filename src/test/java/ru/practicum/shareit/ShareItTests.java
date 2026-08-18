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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShareItTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("Создание пользователя — должен вернуть 201")
    void createUserShouldReturn201() throws Exception {
        UserDto dto = new UserDto(null, "John", "john@mail.com");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    @DisplayName("Создание пользователя — должен вернуть 400 при некорректном email")
    void createUserShouldReturn400WhenEmailInvalid() throws Exception {
        UserDto dto = new UserDto(null, "John", "bad-email");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание пользователя — должен вернуть 409 при дубликате email")
    void createUserShouldReturn409WhenEmailDuplicate() throws Exception {
        UserDto dto = new UserDto(null, "Jane", "jane@mail.com");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        UserDto duplicate = new UserDto(null, "Other", "jane@mail.com");
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Обновление пользователя — должен вернуть 200")
    void updateUserShouldReturn200() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Alice", "alice@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        UserDto update = new UserDto(null, "Alice Updated", "alice-updated@mail.com");
        mockMvc.perform(patch("/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    @Test
    @DisplayName("Поиск пользователя по идентификатору — должен вернуть 200")
    void findUserByIdShouldReturn200() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Bob", "bob@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bob"));
    }

    @Test
    @DisplayName("Получение всех пользователей — должен вернуть 200")
    void findAllUsersShouldReturn200() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "ListUser", "list@mail.com"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Удаление пользователя — должен вернуть 204")
    void deleteUserShouldReturn204() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Charlie", "charlie@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(delete("/users/" + id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Создание вещи — должен вернуть 201")
    void createItemShouldReturn201() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner", "owner@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(created).get("id").asLong();

        ItemDto item = new ItemDto(null, "Drill", "Power drill", true, null, null);
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    @DisplayName("Создание вещи — должен вернуть 404 при отсутствии пользователя")
    void createItemShouldReturn404WhenUserNotFound() throws Exception {
        ItemDto item = new ItemDto(null, "Drill", "Power drill", true, null, null);
        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление вещи — должен вернуть 200")
    void updateItemShouldReturn200() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner2", "owner2@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(created).get("id").asLong();

        String itemCreated = mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Hammer", "Big hammer", true, null, null))))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(itemCreated).get("id").asLong();

        ItemDto update = new ItemDto(null, "Hammer Updated", "Big hammer v2", false, null, null);
        mockMvc.perform(patch("/items/" + itemId)
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hammer Updated"));
    }

    @Test
    @DisplayName("Обновление вещи — должен вернуть 404 при чужом пользователе")
    void updateItemShouldReturn404WhenNotOwner() throws Exception {
        String owner1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner3", "owner3@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(owner1).get("id").asLong();

        String owner2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Other", "other@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long otherId = objectMapper.readTree(owner2).get("id").asLong();

        String itemCreated = mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Saw", "Hand saw", true, null, null))))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(itemCreated).get("id").asLong();

        mockMvc.perform(patch("/items/" + itemId)
                        .header("X-Sharer-User-Id", otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Stolen", "Stolen saw", true, null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Поиск вещи по идентификатору — должен вернуть 200")
    void findItemByIdShouldReturn200() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner4", "owner4@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(created).get("id").asLong();

        String itemCreated = mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Laptop", "Gaming laptop", true, null, null))))
                .andReturn().getResponse().getContentAsString();
        Long itemId = objectMapper.readTree(itemCreated).get("id").asLong();

        mockMvc.perform(get("/items/" + itemId)
                        .header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @DisplayName("Получение вещей владельца — должен вернуть 200")
    void getOwnerItemsShouldReturn200() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner5", "owner5@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Item1", "Desc1", true, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Item1"));
    }

    @Test
    @DisplayName("Поиск вещей — должен вернуть только доступные")
    void searchItemsShouldReturnAvailableOnly() throws Exception {
        String created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UserDto(null, "Owner6", "owner6@mail.com"))))
                .andReturn().getResponse().getContentAsString();
        Long ownerId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "AvailableDrill", "Powerful", true, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "UnavailableDrill", "Broken", false, null, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/items/search").param("text", "Available")
                .header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("AvailableDrill"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Поиск вещей — должен вернуть пустой список для пустого запроса")
    void searchItemsShouldReturnEmptyForBlank() throws Exception {
        mockMvc.perform(get("/items/search").param("text", "")
                        .header("X-Sharer-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Поиск вещи по идентификатору — должен вернуть 404 для несуществующего id")
    void findItemByIdShouldReturn404() throws Exception {
        mockMvc.perform(get("/items/9999")
                        .header("X-Sharer-User-Id", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Поиск пользователя по идентификатору — должен вернуть 404 для несуществующего id")
    void findUserByIdShouldReturn404() throws Exception {
        mockMvc.perform(get("/users/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление вещи — должен вернуть 404 для несуществующей вещи")
    void updateItemShouldReturn404WhenItemNotFound() throws Exception {
        mockMvc.perform(patch("/items/9999")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ItemDto(null, "Name", "Desc", true, null, null))))
                .andExpect(status().isNotFound());
    }
}
