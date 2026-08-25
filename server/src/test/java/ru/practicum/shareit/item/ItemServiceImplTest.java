package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void create_shouldThrowWhenOwnerNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(1L, new ItemDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void create_shouldSaveItem_whenOwnerExists() {
        User owner = new User();
        owner.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        ItemDto dto = new ItemDto(null, "Drill", "Powerful drill", true, null, null);
        Item savedItem = new Item();
        savedItem.setId(1L);
        savedItem.setName("Drill");
        savedItem.setDescription("Powerful drill");
        savedItem.setAvailable(true);
        savedItem.setOwner(owner);

        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        ItemDto result = itemService.create(1L, dto);

        // ownerId должен быть равен 1, т.к. маппер заполняет его из owner
        assertThat(result).isEqualTo(new ItemDto(1L, "Drill", "Powerful drill", true, 1L, null));
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void update_shouldThrowWhenItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.update(1L, 1L, new ItemDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void update_shouldThrowWhenUserIsNotOwner() {
        Item item = new Item();
        User owner = new User();
        owner.setId(2L);
        item.setOwner(owner);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(1L, 1L, new ItemDto()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void update_shouldUpdateFields() {
        User owner = new User();
        owner.setId(1L);
        Item existing = new Item();
        existing.setId(1L);
        existing.setName("Old");
        existing.setDescription("Old desc");
        existing.setAvailable(false);
        existing.setOwner(owner);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(existing));

        ItemDto updateDto = new ItemDto(null, "New", "New desc", true, null, null);
        when(itemRepository.save(any(Item.class))).thenReturn(existing);

        ItemDto result = itemService.update(1L, 1L, updateDto);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getDescription()).isEqualTo("New desc");
        assertThat(result.getAvailable()).isTrue();
        verify(itemRepository, times(1)).save(existing);
    }

    @Test
    void findById_shouldThrowWhenItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void findById_shouldReturnItemViewDto() {
        Item item = new Item();
        item.setId(1L);
        item.setName("Drill");
        item.setDescription("Powerful");
        item.setAvailable(true);
        User owner = new User();
        owner.setId(1L);
        item.setOwner(owner);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemViewDto result = itemService.findById(1L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Drill");
    }

    @Test
    void findAllByOwner_shouldReturnEmptyListWhenNoItems() {
        when(itemRepository.findByOwner_Id(1L)).thenReturn(Collections.emptyList());

        List<ItemViewDto> result = itemService.findAllByOwner(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldReturnEmptyWhenTextBlank() {
        List<ItemDto> result = itemService.search("");
        assertThat(result).isEmpty();

        result = itemService.search(null);
        assertThat(result).isEmpty();
    }

    @Test
    void search_shouldReturnItems_whenTextNotEmpty() {
        String text = "drill";
        User owner = new User();
        owner.setId(1L);
        Item item = new Item();
        item.setId(1L);
        item.setName("Drill");
        item.setDescription("Powerful drill");
        item.setAvailable(true);
        item.setOwner(owner);

        when(itemRepository.search(text)).thenReturn(List.of(item));

        List<ItemDto> result = itemService.search(text);

        // ownerId должен быть равен 1, т.к. маппер заполняет его из owner
        assertThat(result).containsExactly(new ItemDto(1L, "Drill", "Powerful drill", true, 1L, null));
        verify(itemRepository, times(1)).search(text);
    }

    @Test
    void addComment_shouldThrowWhenTextEmpty() {
        CommentDto dto = new CommentDto();
        dto.setText("");

        assertThatThrownBy(() -> itemService.addComment(1L, 1L, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Текст отзыва не может быть пустым");
    }

    @Test
    void addComment_shouldThrowWhenUserNotFound() {
        CommentDto dto = new CommentDto();
        dto.setText("Good");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addComment(1L, 1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void addComment_shouldThrowWhenItemNotFound() {
        CommentDto dto = new CommentDto();
        dto.setText("Good");

        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addComment(1L, 1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void addComment_shouldThrowValidationException_whenBookingNotFinished() {
        CommentDto dto = new CommentDto();
        dto.setText("Good");

        User user = new User();
        user.setId(1L);
        Item item = new Item();
        item.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        // исправлено: используем eq() для всех аргументов
        when(bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                eq(1L), eq(1L), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(1L, 1L, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Отзыв можно оставить только после завершённой аренды вещи");
    }

    @Test
    void addComment_shouldSaveComment_whenValid() {
        CommentDto dto = new CommentDto();
        dto.setText("Good");

        User user = new User();
        user.setId(1L);
        Item item = new Item();
        item.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        // исправлено: используем eq() для всех аргументов
        when(bookingRepository.existsByBooker_IdAndItem_IdAndStatusAndEndBefore(
                eq(1L), eq(1L), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(true);

        Comment savedComment = new Comment();
        savedComment.setId(10L);
        savedComment.setText("Good");
        savedComment.setItem(item);
        savedComment.setAuthor(user);
        savedComment.setCreated(LocalDateTime.now());

        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentDto result = itemService.addComment(1L, 1L, dto);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getText()).isEqualTo("Good");
        verify(commentRepository, times(1)).save(any(Comment.class));
    }
}