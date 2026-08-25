package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.State;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    // ==================== CREATE ====================

    @Test
    void createShouldThrowWhenStartInPast() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().minusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(1));
        dto.setItemId(1L);

        assertThatThrownBy(() -> bookingService.create(1L, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Бронирование можно создавать только на будущую дату");
    }

    @Test
    void createShouldThrowWhenEndBeforeStart() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(2));
        dto.setEnd(LocalDateTime.now().plusHours(1));
        dto.setItemId(1L);

        assertThatThrownBy(() -> bookingService.create(1L, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Дата окончания должна быть позже даты начала");
    }

    @Test
    void createShouldThrowWhenUserNotFound() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));
        dto.setItemId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void createShouldThrowWhenItemNotFound() {
        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));
        dto.setItemId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void createShouldThrowWhenOwnerTriesToBookOwnItem() {
        User owner = new User();
        owner.setId(1L);
        Item item = new Item();
        item.setId(1L);
        item.setOwner(owner);

        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));
        dto.setItemId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(1L, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Вещь с id=1 не найдена");
    }

    @Test
    void createShouldThrowWhenItemNotAvailable() {
        User booker = new User();
        booker.setId(2L);
        User owner = new User();
        owner.setId(1L);
        Item item = new Item();
        item.setId(1L);
        item.setOwner(owner);
        item.setAvailable(false);

        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));
        dto.setItemId(1L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.create(2L, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("недоступна для бронирования");
    }

    @Test
    void createShouldSucceed() {
        User booker = new User();
        booker.setId(2L);
        User owner = new User();
        owner.setId(1L);
        Item item = new Item();
        item.setId(1L);
        item.setOwner(owner);
        item.setAvailable(true);

        BookingCreateDto dto = new BookingCreateDto();
        dto.setStart(LocalDateTime.now().plusHours(1));
        dto.setEnd(LocalDateTime.now().plusHours(2));
        dto.setItemId(1L);

        Booking savedBooking = new Booking();
        savedBooking.setId(10L);
        savedBooking.setItem(item);
        savedBooking.setBooker(booker);
        savedBooking.setStatus(BookingStatus.WAITING);

        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingDto result = bookingService.create(2L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    // ==================== APPROVE ====================

    @Test
    void approveShouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.approve(1L, 1L, true))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Бронирование с id=1 не найдено");
    }

    @Test
    void approveShouldThrowWhenUserIsNotOwner() {
        Booking booking = new Booking();
        Item item = new Item();
        User owner = new User();
        owner.setId(2L);
        item.setOwner(owner);
        booking.setItem(item);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(1L, 1L, true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Подтверждать бронирование может только владелец вещи");
    }

    @Test
    void approveShouldThrowWhenStatusNotWaiting() {
        Booking booking = new Booking();
        Item item = new Item();
        User owner = new User();
        owner.setId(1L);
        item.setOwner(owner);
        booking.setItem(item);
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approve(1L, 1L, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Бронирование уже получило решение владельца");
    }

    @Test
    void approveShouldSetApproved() {
        Booking booking = new Booking();
        Item item = new Item();
        item.setId(10L);
        User owner = new User();
        owner.setId(1L);
        item.setOwner(owner);
        booking.setItem(item);
        User booker = new User();
        booker.setId(2L);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.approve(1L, 1L, true);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    void approveShouldSetRejected() {
        Booking booking = new Booking();
        Item item = new Item();
        item.setId(10L);
        User owner = new User();
        owner.setId(1L);
        item.setOwner(owner);
        booking.setItem(item);
        User booker = new User();
        booker.setId(2L);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto result = bookingService.approve(1L, 1L, false);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
        verify(bookingRepository, times(1)).save(booking);
    }

    // ==================== FIND BY ID ====================

    @Test
    void findByIdShouldThrowWhenBookingNotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findById(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Бронирование с id=1 не найдено");
    }

    @Test
    void findByIdShouldThrowWhenUserIsNeitherBookerNorOwner() {
        Booking booking = new Booking();
        User booker = new User();
        booker.setId(2L);
        booking.setBooker(booker);
        Item item = new Item();
        User owner = new User();
        owner.setId(3L);
        item.setOwner(owner);
        booking.setItem(item);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.findById(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Бронирование с id=1 не найдено");
    }

    @Test
    void findByIdShouldReturnForBooker() {
        Booking booking = new Booking();
        User booker = new User();
        booker.setId(1L);
        booking.setBooker(booker);
        Item item = new Item();
        User owner = new User();
        owner.setId(2L);
        item.setOwner(owner);
        booking.setItem(item);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.findById(1L, 1L);

        assertThat(result).isNotNull();
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    void findByIdShouldReturnForOwner() {
        Booking booking = new Booking();
        User booker = new User();
        booker.setId(2L);
        booking.setBooker(booker);
        Item item = new Item();
        User owner = new User();
        owner.setId(1L);
        item.setOwner(owner);
        booking.setItem(item);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        BookingDto result = bookingService.findById(1L, 1L);

        assertThat(result).isNotNull();
        verify(bookingRepository, times(1)).findById(1L);
    }

    // ==================== FIND ALL BY BOOKER ====================

    @Test
    void findAllByBookerShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findAllByBooker(1L, State.ALL))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void findAllByBookerShouldReturnAll() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_Id(eq(1L), any(Sort.class)))
                .thenReturn(List.of(booking, booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.ALL);

        assertThat(result).hasSize(2);
        verify(bookingRepository, times(1)).findByBooker_Id(eq(1L), any(Sort.class));
    }

    @Test
    void findAllByBookerShouldReturnCurrent() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_IdAndStartIsBeforeAndEndIsAfter(eq(1L), any(), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.CURRENT);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBooker_IdAndStartIsBeforeAndEndIsAfter(eq(1L), any(), any(), any(Sort.class));
    }

    @Test
    void findAllByBookerShouldReturnPast() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_IdAndEndIsBefore(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.PAST);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBooker_IdAndEndIsBefore(eq(1L), any(), any(Sort.class));
    }

    @Test
    void findAllByBookerShouldReturnFuture() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_IdAndStartIsAfter(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.FUTURE);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBooker_IdAndStartIsAfter(eq(1L), any(), any(Sort.class));
    }

    @Test
    void findAllByBookerShouldReturnWaiting() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_IdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.WAITING);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBooker_IdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class));
    }

    @Test
    void findAllByBookerShouldReturnRejected() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByBooker_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByBooker(1L, State.REJECTED);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByBooker_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class));
    }

    // ==================== FIND ALL BY OWNER ====================

    @Test
    void findAllByOwnerShouldThrowWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.findAllByOwner(1L, State.ALL))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с id=1 не найден");
    }

    @Test
    void findAllByOwnerShouldReturnAll() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_Id(eq(1L), any(Sort.class)))
                .thenReturn(List.of(booking, booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.ALL);

        assertThat(result).hasSize(2);
        verify(bookingRepository, times(1)).findByItem_Owner_Id(eq(1L), any(Sort.class));
    }

    @Test
    void findAllByOwnerShouldReturnCurrent() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(eq(1L), any(), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.CURRENT);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(eq(1L), any(), any(), any(Sort.class));
    }

    @Test
    void findAllByOwnerShouldReturnPast() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_IdAndEndIsBefore(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.PAST);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItem_Owner_IdAndEndIsBefore(eq(1L), any(), any(Sort.class));
    }

    @Test
    void findAllByOwnerShouldReturnFuture() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_IdAndStartIsAfter(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.FUTURE);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItem_Owner_IdAndStartIsAfter(eq(1L), any(), any(Sort.class));
    }

    @Test
    void findAllByOwnerShouldReturnWaiting() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.WAITING);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class));
    }

    @Test
    void findAllByOwnerShouldReturnRejected() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking booking = createBookingWithItemAndBooker();
        when(bookingRepository.findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class)))
                .thenReturn(List.of(booking));

        List<BookingDto> result = bookingService.findAllByOwner(1L, State.REJECTED);

        assertThat(result).hasSize(1);
        verify(bookingRepository, times(1))
                .findByItem_Owner_IdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class));
    }

    // ==================== HELPER ====================

    private Booking createBookingWithItemAndBooker() {
        Booking booking = new Booking();
        Item item = new Item();
        item.setId(10L);
        User owner = new User();
        owner.setId(100L);
        item.setOwner(owner);
        booking.setItem(item);
        User booker = new User();
        booker.setId(200L);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);
        return booking;
    }
}