package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.util.List;

public interface ItemRequestService {

    ItemRequestDto create(Long userId, ItemRequestCreateDto dto);

    List<ItemRequestDto> findAllByRequestor(Long userId);

    List<ItemRequestDto> findAllOthers(Long userId);

    ItemRequestDto findById(Long userId, Long requestId);
}
