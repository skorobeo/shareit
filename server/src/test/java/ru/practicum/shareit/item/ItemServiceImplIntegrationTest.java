package ru.practicum.shareit.item;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemViewDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ItemServiceImplIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;

    private UserDto createUser(String email) {
        return userService.create(new UserDto(null, email.split("@")[0], email));
    }

    private Long createItem(Long ownerId, String name) {
        return itemService.create(ownerId,
                new ItemDto(null, name, "desc-" + name, true, null, null)).getId();
    }

    @Test
    void createShouldAssignIdAndOwner() {
        UserDto owner = createUser("item-owner@mail.com");

        ItemDto dto = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Мощная", true, null, null));

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getName()).isEqualTo("Дрель");
        assertThat(dto.getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void createWithRequestIdShouldPersistRequestId() {
        UserDto owner = createUser("item-owner2@mail.com");

        ItemDto dto = itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Мощная", true, null, 7L));

        assertThat(dto.getRequestId()).isEqualTo(7L);
    }

    @Test
    void createWithUnknownOwnerShouldThrow() {
        assertThatThrownBy(() -> itemService.create(999L,
                new ItemDto(null, "Дрель", "Мощная", true, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateShouldChangeOnlyNonNullFields() {
        UserDto owner = createUser("item-owner3@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        ItemDto updated = itemService.update(owner.getId(), itemId,
                new ItemDto(null, "Шуруповерт", "Новое описание", false, null, null));

        assertThat(updated.getName()).isEqualTo("Шуруповерт");
        assertThat(updated.getDescription()).isEqualTo("Новое описание");
        assertThat(updated.getAvailable()).isFalse();
    }

    @Test
    void updateByNonOwnerShouldThrow() {
        UserDto owner = createUser("item-owner4@mail.com");
        UserDto other = createUser("item-other@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        assertThatThrownBy(() -> itemService.update(other.getId(), itemId,
                new ItemDto(null, "X", "Y", true, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateNonexistentItemShouldThrow() {
        UserDto owner = createUser("item-owner5@mail.com");

        assertThatThrownBy(() -> itemService.update(owner.getId(), 999L,
                new ItemDto(null, "X", "Y", true, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdShouldReturnItemWithoutBookingsForNonOwner() {
        UserDto owner = createUser("item-owner6@mail.com");
        UserDto booker = createUser("item-booker@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        ItemViewDto view = itemService.findById(booker.getId(), itemId);

        assertThat(view.getId()).isEqualTo(itemId);
        assertThat(view.getLastBooking()).isNull();
        assertThat(view.getNextBooking()).isNull();
        assertThat(view.getComments()).isEmpty();
    }

    @Test
    void findByIdNonexistentItemShouldThrow() {
        UserDto owner = createUser("item-owner7@mail.com");

        assertThatThrownBy(() -> itemService.findById(owner.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getUserItemsShouldIncludeLastNextBookingsAndComments() {
        UserDto owner = createUser("item-owner8@mail.com");
        UserDto booker = createUser("item-booker8@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        BookingDto future = bookingService.create(booker.getId(),
                new BookingCreateDto(itemId, now.plusDays(1), now.plusDays(2)));
        bookingService.approve(owner.getId(), future.getId(), true);

        Item item = itemRepository.findById(itemId).orElseThrow();
        User ownerEntity = userRepository.findById(owner.getId()).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        bookingRepository.save(new Booking(null, now.minusDays(2), now.minusDays(1),
                item, bookerEntity, BookingStatus.APPROVED));

        List<ItemViewDto> items = itemService.findAllByOwner(owner.getId());

        assertThat(items).hasSize(1);
        ItemViewDto view = items.get(0);
        assertThat(view.getLastBooking()).isNotNull();
        assertThat(view.getLastBooking().getBookerId()).isEqualTo(booker.getId());
        assertThat(view.getNextBooking()).isNotNull();
        assertThat(view.getNextBooking().getBookerId()).isEqualTo(booker.getId());
    }

    @Test
    void getUserItemsShouldReturnEmptyForOwnerWithoutItems() {
        UserDto owner = createUser("item-owner9@mail.com");

        assertThat(itemService.findAllByOwner(owner.getId())).isEmpty();
    }

    @Test
    void searchShouldFindAvailableItemsByNameOrDescription() {
        UserDto owner = createUser("item-owner10@mail.com");
        createItem(owner.getId(), "Дрель");
        createItem(owner.getId(), "Пила");

        assertThat(itemService.search("дрел")).hasSize(1);
        assertThat(itemService.search("ДРЕЛ")).hasSize(1);
        assertThat(itemService.search("desc-пила")).hasSize(1);
    }

    @Test
    void searchShouldReturnEmptyForBlankText() {
        assertThat(itemService.search("")).isEmpty();
        assertThat(itemService.search("   ")).isEmpty();
        assertThat(itemService.search(null)).isEmpty();
    }

    @Test
    void addCommentShouldSucceedAfterFinishedBooking() {
        UserDto owner = createUser("item-owner11@mail.com");
        UserDto booker = createUser("item-booker11@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");
        LocalDateTime now = LocalDateTime.now();

        Item item = itemRepository.findById(itemId).orElseThrow();
        User bookerEntity = userRepository.findById(booker.getId()).orElseThrow();
        bookingRepository.save(new Booking(null, now.minusDays(2), now.minusDays(1),
                item, bookerEntity, BookingStatus.APPROVED));

        CommentDto comment = itemService.addComment(booker.getId(), itemId,
                new CommentDto(null, "Отличная вещь", null, null));

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getText()).isEqualTo("Отличная вещь");
        assertThat(comment.getAuthorName()).isEqualTo("item-booker11");
        assertThat(comment.getItemId()).isEqualTo(itemId);
        assertThat(comment.getCreated()).isNotNull();
    }

    @Test
    void addCommentWithBlankTextShouldThrow() {
        UserDto owner = createUser("item-owner12@mail.com");
        UserDto booker = createUser("item-booker12@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        assertThatThrownBy(() -> itemService.addComment(booker.getId(), itemId,
                new CommentDto(null, "  ", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addCommentWithoutFinishedBookingShouldThrow() {
        UserDto owner = createUser("item-owner13@mail.com");
        UserDto booker = createUser("item-booker13@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        assertThatThrownBy(() -> itemService.addComment(booker.getId(), itemId,
                new CommentDto(null, "Отличная вещь", null, null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addCommentWithUnknownUserShouldThrow() {
        UserDto owner = createUser("item-owner14@mail.com");
        Long itemId = createItem(owner.getId(), "Дрель");

        assertThatThrownBy(() -> itemService.addComment(999L, itemId,
                new CommentDto(null, "Отличная вещь", null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addCommentWithUnknownItemShouldThrow() {
        UserDto owner = createUser("item-owner15@mail.com");

        assertThatThrownBy(() -> itemService.addComment(owner.getId(), 999L,
                new CommentDto(null, "Отличная вещь", null, null)))
                .isInstanceOf(NotFoundException.class);
    }
}