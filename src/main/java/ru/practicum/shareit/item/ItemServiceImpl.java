package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemBookingDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;
import ru.practicum.shareit.item.model.Comment;
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
    private final CommentRepository commentRepository;

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
            throw new NotFoundException("Вещь с id=" + itemId + " не найдена");
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
    public ItemViewDto findById(Long userId, Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException("Вещь с id=" + itemId + " не найдена"));
        boolean isOwner = item.getOwner().getId().equals(userId);
        return toItemViewDto(item, isOwner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemBookingDto> findAllByOwner(Long ownerId) {    //  все вещи владельца
        return itemRepository.findByOwner_Id(ownerId).stream()
                .map(item -> ItemMapper.toItemBookingDto(
                        item,
                        findLastBooking(item.getId()),
                        findNextBooking(item.getId()),
                        findComments(item.getId())))
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

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Текст отзыва не может быть пустым");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + itemId + " не найдена"));

        boolean rentedAndFinished = bookingRepository
                .existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                        userId, itemId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!rentedAndFinished) {
            throw new ValidationException("Отзыв можно оставить только после завершённой аренды вещи");
        }

        Comment comment = new Comment(null, commentDto.getText(), item, author, LocalDateTime.now());
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    private ItemViewDto toItemViewDto(Item item, boolean isOwner) {
        BookingShortDto lastBooking = null;
        BookingShortDto nextBooking = null;
        if (isOwner) {
            lastBooking = findLastBooking(item.getId());
            nextBooking = findNextBooking(item.getId());
        }
        return ItemMapper.toItemViewDto(item, lastBooking, nextBooking, findComments(item.getId()));
    }

    private BookingShortDto findLastBooking(Long itemId) {
        return bookingRepository
                .findFirstByItem_IdAndStatusAndEndBeforeOrderByEndDesc(
                        itemId, BookingStatus.APPROVED, LocalDateTime.now())
                .map(BookingMapper::toShortDto)
                .orElse(null);
    }

    private BookingShortDto findNextBooking(Long itemId) {
        return bookingRepository
                .findFirstByItem_IdAndStatusAndStartAfterOrderByStartAsc(
                        itemId, BookingStatus.APPROVED, LocalDateTime.now())
                .map(BookingMapper::toShortDto)
                .orElse(null);
    }

    private List<CommentDto> findComments(Long itemId) {
        return commentRepository.findByItem_Id(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }
}