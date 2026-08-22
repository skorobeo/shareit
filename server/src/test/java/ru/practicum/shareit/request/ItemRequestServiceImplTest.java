package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl requestService;

    @Test
    void createShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.create(1L, new ItemRequestCreateDto("description")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void createShouldSucceed() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ItemRequest savedRequest = new ItemRequest();
        savedRequest.setId(10L);
        savedRequest.setDescription("description");
        savedRequest.setRequestor(user);
        savedRequest.setCreated(LocalDateTime.now());

        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(savedRequest);

        ItemRequestDto result = requestService.create(1L, new ItemRequestCreateDto("description"));

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("description");
        assertThat(result.getRequestorId()).isEqualTo(1L);
        assertThat(result.getCreated()).isNotNull();
        verify(itemRequestRepository, times(1)).save(any(ItemRequest.class));
    }

    @Test
    void findAllByRequestorShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.findAllByRequestor(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void findAllByRequestorShouldReturnRequests() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ItemRequest request = new ItemRequest();
        request.setId(1L);
        request.setDescription("desc");
        request.setRequestor(user);
        request.setCreated(LocalDateTime.now());

        when(itemRequestRepository.findAllByRequestor_IdOrderByCreatedDesc(1L))
                .thenReturn(List.of(request));
        when(itemRepository.findByRequestId(1L)).thenReturn(Collections.emptyList());

        List<ItemRequestDto> result = requestService.findAllByRequestor(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        verify(itemRequestRepository, times(1)).findAllByRequestor_IdOrderByCreatedDesc(1L);
        verify(itemRepository, times(1)).findByRequestId(1L);
    }

    @Test
    void findAllOthersShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.findAllOthers(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void findAllOthersShouldReturnRequests() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ItemRequest request = new ItemRequest();
        request.setId(1L);
        request.setDescription("desc");
        request.setRequestor(new User());
        request.setCreated(LocalDateTime.now());

        when(itemRequestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(1L))
                .thenReturn(List.of(request));
        when(itemRepository.findByRequestId(1L)).thenReturn(Collections.emptyList());

        List<ItemRequestDto> result = requestService.findAllOthers(1L);

        assertThat(result).hasSize(1);
        verify(itemRequestRepository, times(1)).findAllByRequestor_IdNotOrderByCreatedDesc(1L);
        verify(itemRepository, times(1)).findByRequestId(1L);
    }

    @Test
    void findByIdShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.findById(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void findByIdShouldThrowWhenRequestNotFound() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.findById(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Запрос с id=1 не найден");
    }

    @Test
    void findByIdShouldReturnRequest() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ItemRequest request = new ItemRequest();
        request.setId(1L);
        request.setDescription("desc");
        request.setRequestor(user);
        request.setCreated(LocalDateTime.now());

        when(itemRequestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestId(1L)).thenReturn(Collections.emptyList());

        ItemRequestDto result = requestService.findById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(itemRequestRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).findByRequestId(1L);
    }
}