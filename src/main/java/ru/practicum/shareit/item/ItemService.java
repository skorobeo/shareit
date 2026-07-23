package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {

    ItemDto create(Long ownerId, ItemDto itemDto);

    ItemDto update(Long ownerId, Long itemId, ItemDto itemDto);

    ItemDto findById(Long itemId);

    List<ItemDto> findAllByOwner(Long ownerId);

    List<ItemDto> search(String text);
}
