package ru.practicum.shareit.client;

import ru.practicum.shareit.Constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ItemClient extends BaseClient {
    public ItemClient(@Value("${shareit-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        super(serverUrl, builder);
    }

    private HttpHeaders headersWithUserId(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Constants.USER_ID_HEADER, String.valueOf(userId));
        return headers;
    }

    public ResponseEntity<String> createItem(long userId, ItemDto itemDto) {
        return post("/items", headersWithUserId(userId), itemDto);
    }

    public ResponseEntity<String> updateItem(long userId, long itemId, ItemDto itemDto) {
        return patch("/items/" + itemId, headersWithUserId(userId), itemDto);
    }

    public ResponseEntity<String> getItem(long itemId, long userId) {
        return get("/items/" + itemId, headersWithUserId(userId));
    }

    public ResponseEntity<String> getItemsByUser(long userId) {
        return get("/items", headersWithUserId(userId));
    }

    public ResponseEntity<String> searchItems(long userId, String text) {
        // Кодируем текст для безопасной передачи в URL
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        return get("/items/search?text=" + encodedText, headersWithUserId(userId));
    }

    public ResponseEntity<String> addComment(long userId, long itemId, CommentDto commentDto) {
        return post("/items/" + itemId + "/comment", headersWithUserId(userId), commentDto);
    }
}