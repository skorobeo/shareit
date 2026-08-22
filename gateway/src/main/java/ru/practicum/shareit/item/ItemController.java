package ru.practicum.shareit.item;
import ru.practicum.shareit.Constants;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.ItemClient;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<String> createItem(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                             @Valid @RequestBody ItemDto itemDto) {
        return itemClient.createItem(userId, itemDto);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<String> updateItem(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                             @PathVariable long itemId,
                                             @RequestBody ItemDto itemDto) {
        return itemClient.updateItem(userId, itemId, itemDto);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<String> getItem(@PathVariable long itemId,
                                          @RequestHeader(Constants.USER_ID_HEADER) long userId) {
        return itemClient.getItem(itemId, userId);
    }

    @GetMapping
    public ResponseEntity<String> getItemsByUser(@RequestHeader(Constants.USER_ID_HEADER) long userId) {
        return itemClient.getItemsByUser(userId);
    }

    @GetMapping("/search")
    public ResponseEntity<String> searchItems(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                              @RequestParam String text) {
        return itemClient.searchItems(userId, text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<String> addComment(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                             @PathVariable long itemId,
                                             @Valid @RequestBody CommentDto commentDto) {
        return itemClient.addComment(userId, itemId, commentDto);
    }
}