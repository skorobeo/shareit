package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestCreateDto dto) {
        User requestor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        ItemRequest request = ItemRequestMapper.toItemRequest(dto, requestor);
        request.setCreated(LocalDateTime.now());  // <-- добавляем дату создания
        request = itemRequestRepository.save(request);
        return ItemRequestMapper.toItemRequestDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestDto> findAllByRequestor(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        return itemRequestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId).stream()
                .map(this::toDtoWithItems)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRequestDto> findAllOthers(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        return itemRequestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(userId).stream()
                .map(this::toDtoWithItems)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequestDto findById(Long userId, Long requestId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id=" + requestId + " не найден"));
        return toDtoWithItems(request);
    }

    private ItemRequestDto toDtoWithItems(ItemRequest request) {
        ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request);
        dto.setItems(itemRepository.findByRequestId(request.getId()).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList()));
        return dto;
    }
}