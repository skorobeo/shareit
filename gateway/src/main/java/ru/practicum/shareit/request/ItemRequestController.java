package ru.practicum.shareit.request;
import ru.practicum.shareit.Constants;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.client.RequestClient;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final RequestClient requestClient;

    @PostMapping
    public ResponseEntity<String> createRequest(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                                @Valid @RequestBody ItemRequestCreateDto requestDto) {
        return requestClient.createRequest(userId, requestDto);
    }

    @GetMapping
    public ResponseEntity<String> getOwnRequests(@RequestHeader(Constants.USER_ID_HEADER) long userId) {
        return requestClient.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<String> getAllRequests(@RequestHeader(Constants.USER_ID_HEADER) long userId) {
        return requestClient.getAllRequests(userId);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<String> getRequest(@RequestHeader(Constants.USER_ID_HEADER) long userId,
                                             @PathVariable long requestId) {
        return requestClient.getRequest(userId, requestId);
    }
}