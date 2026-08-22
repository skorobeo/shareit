package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto dto) {
        if (dto.getStart().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Бронирование можно создавать только на будущую дату");
        }
        if (!dto.getStart().isBefore(dto.getEnd())) {
            throw new ValidationException("Дата окончания должна быть позже даты начала");
        }

        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id=" + dto.getItemId() + " не найдена"));

        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Вещь с id=" + dto.getItemId() + " не найдена");
        }

        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new ValidationException("Вещь с id=" + dto.getItemId() + " недоступна для бронирования");
        }

        Booking booking = BookingMapper.toBooking(dto);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStatus(BookingStatus.WAITING);

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, Boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));

        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("Подтверждать бронирование может только владелец вещи");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Бронирование уже получило решение владельца");
        }

        booking.setStatus(Boolean.TRUE.equals(approved)
                ? BookingStatus.APPROVED
                : BookingStatus.REJECTED);

        booking = bookingRepository.save(booking);

        return BookingMapper.toBookingDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto findById(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id=" + bookingId + " не найдено"));

        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Бронирование с id=" + bookingId + " не найдено");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> findAllByBooker(Long userId, State state) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Sort sort = Sort.by(Sort.Direction.DESC, "start");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBooker_Id(userId, sort);
            case CURRENT -> bookingRepository
                    .findByBooker_IdAndStartIsBeforeAndEndIsAfter(userId, now, now, sort);
            case PAST -> bookingRepository.findByBooker_IdAndEndIsBefore(userId, now, sort);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartIsAfter(userId, now, sort);
            case WAITING -> bookingRepository
                    .findByBooker_IdAndStatus(userId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository
                    .findByBooker_IdAndStatus(userId, BookingStatus.REJECTED, sort);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> findAllByOwner(Long ownerId, State state) {
        userRepository.findById(ownerId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + ownerId + " не найден"));

        Sort sort = Sort.by(Sort.Direction.DESC, "start");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItem_Owner_Id(ownerId, sort);
            case CURRENT -> bookingRepository
                    .findByItem_Owner_IdAndStartIsBeforeAndEndIsAfter(ownerId, now, now, sort);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndIsBefore(ownerId, now, sort);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartIsAfter(ownerId, now, sort);
            case WAITING -> bookingRepository
                    .findByItem_Owner_IdAndStatus(ownerId, BookingStatus.WAITING, sort);
            case REJECTED -> bookingRepository
                    .findByItem_Owner_IdAndStatus(ownerId, BookingStatus.REJECTED, sort);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }
}