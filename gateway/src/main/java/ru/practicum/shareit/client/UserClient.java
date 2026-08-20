package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.user.dto.UserDto;

@Service
public class UserClient extends BaseClient {
    public UserClient(@Value("${shareit-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        super(serverUrl, builder);
    }

    public ResponseEntity<String> createUser(UserDto userDto) {
        return post("/users", userDto);
    }

    public ResponseEntity<String> updateUser(long userId, UserDto userDto) {
        return patch("/users/" + userId, userDto);
    }

    public ResponseEntity<String> getUser(long userId) {
        return get("/users/" + userId);
    }

    public ResponseEntity<String> getAllUsers() {
        return get("/users");
    }

    public ResponseEntity<String> deleteUser(long userId) {
        return delete("/users/" + userId);
    }
}