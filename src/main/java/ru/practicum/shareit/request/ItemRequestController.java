package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    private static final String UserIdHeader = "X-Sharer-User-Id";

    @PostMapping
    public ItemRequestDto create(@RequestHeader(UserIdHeader) Long userId,
                                 @Valid @RequestBody ItemRequestCreateDto dto) {
        return itemRequestService.create(userId, dto);
    }

    @GetMapping
    public List<ItemRequestDto> findAllByRequestor(@RequestHeader(UserIdHeader) Long userId) {
        return itemRequestService.findAllByRequestor(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> findAllOthers(@RequestHeader(UserIdHeader) Long userId) {
        return itemRequestService.findAllOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto findById(@RequestHeader(UserIdHeader) Long userId,
                                   @PathVariable Long requestId) {
        return itemRequestService.findById(userId, requestId);
    }
}
