package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.State;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BookingServiceImplIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private UserDto createUser(String email) {
        return userService.create(new UserDto(null, email.split("@")[0], email));
    }

    private Long createAvailableItem(Long ownerId, String name) {
        return itemService.create(ownerId,
                new ItemDto(null, name, "desc-" + name, true, null, null)).getId();
    }

    @Test
    void createShouldSetWaitingStatus() {
        UserDto owner = createUser("bk-owner@mail.com");
        UserDto booker = createUser("bk-booker@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto dto = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(dto.getItem().getId()).isEqualTo(itemId);
        assertThat(dto.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void createWithStartInPastShouldThrow() {
        UserDto owner = createUser("bk-owner1@mail.com");
        UserDto booker = createUser("bk-booker1@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.minusDays(1), now.plusDays(1))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWithEndBeforeStartShouldThrow() {
        UserDto owner = createUser("bk-owner2@mail.com");
        UserDto booker = createUser("bk-booker2@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(2), now.plusDays(1))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWithUnknownBookerShouldThrow() {
        UserDto owner = createUser("bk-owner3@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(999L,
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createWithUnknownItemShouldThrow() {
        UserDto booker = createUser("bk-booker3@mail.com");
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(booker.getId(),
                new BookingCreateDto(999L, now.plusDays(1), now.plusDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createForOwnItemShouldThrow() {
        UserDto owner = createUser("bk-owner4@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(owner.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createForUnavailableItemShouldThrow() {
        UserDto owner = createUser("bk-owner5@mail.com");
        UserDto booker = createUser("bk-booker5@mail.com");
        Long itemId = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "desc", false, null, null)).getId();
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void approveByOwnerShouldApprove() {
        UserDto owner = createUser("bk-owner6@mail.com");
        UserDto booker = createUser("bk-booker6@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto created = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approveByNonOwnerShouldThrow() {
        UserDto owner = createUser("bk-owner7@mail.com");
        UserDto booker = createUser("bk-booker7@mail.com");
        UserDto stranger = createUser("bk-stranger@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto created = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));

        assertThatThrownBy(() -> bookingService.approve(stranger.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approveAlreadyDecidedBookingShouldThrow() {
        UserDto owner = createUser("bk-owner8@mail.com");
        UserDto booker = createUser("bk-booker8@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto created = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));
        bookingService.approve(owner.getId(), created.getId(), true);

        assertThatThrownBy(() -> bookingService.approve(owner.getId(), created.getId(), false))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void approveUnknownBookingShouldThrow() {
        UserDto owner = createUser("bk-owner9@mail.com");

        assertThatThrownBy(() -> bookingService.approve(owner.getId(), 999L, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdShouldReturnForBookerAndOwnerButNotOther() {
        UserDto owner = createUser("bk-owner10@mail.com");
        UserDto booker = createUser("bk-booker10@mail.com");
        UserDto stranger = createUser("bk-stranger10@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto created = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));

        assertThat(bookingService.findById(booker.getId(), created.getId()).getId())
                .isEqualTo(created.getId());
        assertThat(bookingService.findById(owner.getId(), created.getId()).getId())
                .isEqualTo(created.getId());
        assertThatThrownBy(() -> bookingService.findById(stranger.getId(), created.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdUnknownBookingShouldThrow() {
        UserDto owner = createUser("bk-owner11@mail.com");

        assertThatThrownBy(() -> bookingService.findById(owner.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllByBookerShouldFilterByState() {
        UserDto owner = createUser("bk-owner12@mail.com");
        UserDto booker = createUser("bk-booker12@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        User ownerEntity = userRepository.findById(owner.getId()).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        Item itemEntity = itemRepository.findById(itemId).orElseThrow();

        bookingRepository.save(new Booking(null, now.plusDays(1), now.plusDays(2),
                itemEntity, bookerEntity, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.minusDays(2), now.minusDays(1),
                itemEntity, bookerEntity, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.minusDays(1), now.plusDays(1),
                itemEntity, bookerEntity, BookingStatus.WAITING));
        bookingRepository.save(new Booking(null, now.minusDays(1), now.minusDays(1).plusHours(1),
                itemEntity, bookerEntity, BookingStatus.REJECTED));

        assertThat(bookingService.findAllByBooker(booker.getId(), State.ALL)).hasSize(4);
        assertThat(bookingService.findAllByBooker(booker.getId(), State.FUTURE)).hasSize(1);
        assertThat(bookingService.findAllByBooker(booker.getId(), State.PAST)).hasSize(2);
        assertThat(bookingService.findAllByBooker(booker.getId(), State.CURRENT)).hasSize(1);
        assertThat(bookingService.findAllByBooker(booker.getId(), State.WAITING)).hasSize(1);
        assertThat(bookingService.findAllByBooker(booker.getId(), State.REJECTED)).hasSize(1);
    }

    @Test
    void findAllByBookerUnknownUserShouldThrow() {
        assertThatThrownBy(() -> bookingService.findAllByBooker(999L, State.ALL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllByOwnerShouldFilterByState() {
        UserDto owner = createUser("bk-owner13@mail.com");
        UserDto booker = createUser("bk-booker13@mail.com");
        Long itemId = createAvailableItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        User ownerEntity = userRepository.findById(owner.getId()).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        Item itemEntity = itemRepository.findById(itemId).orElseThrow();

        bookingRepository.save(new Booking(null, now.plusDays(1), now.plusDays(2),
                itemEntity, bookerEntity, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.minusDays(2), now.minusDays(1),
                itemEntity, bookerEntity, BookingStatus.APPROVED));
        bookingRepository.save(new Booking(null, now.minusDays(1), now.plusDays(1),
                itemEntity, bookerEntity, BookingStatus.WAITING));

        assertThat(bookingService.findAllByOwner(owner.getId(), State.ALL)).hasSize(3);
        assertThat(bookingService.findAllByOwner(owner.getId(), State.FUTURE)).hasSize(1);
        assertThat(bookingService.findAllByOwner(owner.getId(), State.PAST)).hasSize(1);
        assertThat(bookingService.findAllByOwner(owner.getId(), State.CURRENT)).hasSize(1);
        assertThat(bookingService.findAllByOwner(owner.getId(), State.WAITING)).hasSize(1);
        assertThat(bookingService.findAllByOwner(owner.getId(), State.REJECTED)).isEmpty();
    }

    @Test
    void findAllByOwnerUnknownUserShouldThrow() {
        assertThatThrownBy(() -> bookingService.findAllByOwner(999L, State.ALL))
                .isInstanceOf(NotFoundException.class);
    }
}