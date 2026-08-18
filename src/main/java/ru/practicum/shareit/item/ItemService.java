package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemBookingDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;

import java.util.List;

public interface ItemService {

    ItemDto create(Long ownerId, ItemDto itemDto);

    ItemDto update(Long ownerId, Long itemId, ItemDto itemDto);

    ItemViewDto findById(Long userId, Long itemId);

    List<ItemBookingDto> findAllByOwner(Long ownerId);

    List<ItemDto> search(String text);   // ищем только доступные вещи, возвращаем список

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);
}