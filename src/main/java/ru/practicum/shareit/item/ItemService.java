package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.ItemBookingDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {

    ItemDto create(Long ownerId, ItemDto itemDto);

    ItemDto update(Long ownerId, Long itemId, ItemDto itemDto);

    ItemBookingDto findById(Long userId, Long itemId);

    List<ItemBookingDto> findAllByOwner(Long ownerId);

    List<ItemDto> search(String text);   // ищем только доступные вещи, возвращаем список
}