package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemBookingDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + ownerId + " не найден"));

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        item = itemRepository.save(item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto update(Long ownerId, Long itemId, ItemDto itemDto) {
        Item existing = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        if (!existing.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Редактировать вещь может только её владелец");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            existing.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            existing.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            existing.setAvailable(itemDto.getAvailable());
        }

        itemRepository.save(existing);
        return ItemMapper.toItemDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemBookingDto findById(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        // даты бронирований показывает только владельцу вещи
        boolean isOwner = item.getOwner().getId().equals(userId);
        return toItemBookingDto(item, isOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemBookingDto> findAllByOwner(Long ownerId) {    //  все вещи владельца
        return itemRepository.findByOwner_Id(ownerId).stream()
                .map(item -> toItemBookingDto(item, true))
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    private ItemBookingDto toItemBookingDto(Item item, boolean isOwner) {
        BookingShortDto lastBooking = null;
        BookingShortDto nextBooking = null;
        if (isOwner) {
            LocalDateTime now = LocalDateTime.now();
            lastBooking = bookingRepository
                    .findFirstByItem_IdAndStatusAndEndBeforeOrderByEndDesc(
                            item.getId(), BookingStatus.APPROVED, now)
                    .map(BookingMapper::toShortDto)
                    .orElse(null);
            nextBooking = bookingRepository
                    .findFirstByItem_IdAndStatusAndStartAfterOrderByStartAsc(
                            item.getId(), BookingStatus.APPROVED, now)
                    .map(BookingMapper::toShortDto)
                    .orElse(null);
        }
        return ItemMapper.toItemBookingDto(item, lastBooking, nextBooking);
    }
}