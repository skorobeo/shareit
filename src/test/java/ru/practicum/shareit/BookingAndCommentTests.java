package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingAndCommentTests {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Long createUser(String name) throws Exception {
        int n = SEQ.incrementAndGet();
        UserDto dto = new UserDto(null, name, "user" + n + "@mail.com");
        String body = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createItem(Long ownerId, String name, boolean available) throws Exception {
        ItemDto dto = new ItemDto(null, name, name + " description", available, null, null);
        String body = mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createBooking(Long bookerId, Long itemId,
                               LocalDateTime start, LocalDateTime end) throws Exception {
        BookingCreateDto dto = new BookingCreateDto(itemId, start, end);
        String body = mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long seedBooking(Long itemId, Long bookerId,
                             LocalDateTime start, LocalDateTime end, BookingStatus status) {
        User booker = userRepository.findById(bookerId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();
        return bookingRepository
                .save(new Booking(null, start, end, item, booker, status)).getId();
    }

    private void approve(Long ownerId, Long bookingId, boolean approved) throws Exception {
        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", ownerId)
                        .param("approved", String.valueOf(approved)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 200 и статус WAITING")
    void createBookingShouldReturn200() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.item.id").value(itemId))
                .andExpect(jsonPath("$.booker.id").value(bookerId));
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 400 при начале в прошлом")
    void createBookingShouldReturn400WhenStartInPast() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 400 при окончании раньше начала")
    void createBookingShouldReturn400WhenEndBeforeStart() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 404 при несуществующем пользователе")
    void createBookingShouldReturn404WhenUserNotFound() throws Exception {
        Long ownerId = createUser("Owner");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 404 при несуществующей вещи")
    void createBookingShouldReturn404WhenItemNotFound() throws Exception {
        Long bookerId = createUser("Booker");

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                999999L, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 404 при бронировании своей вещи")
    void createBookingShouldReturn404WhenOwnItem() throws Exception {
        Long ownerId = createUser("Owner");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание бронирования — должен вернуть 400 при недоступной вещи")
    void createBookingShouldReturn400WhenItemNotAvailable() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", false);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto(
                                itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Подтверждение бронирования — должен вернуть 200 и статус APPROVED")
    void approveBookingShouldReturnApproved() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", ownerId)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("Отклонение бронирования — должен вернуть 200 и статус REJECTED")
    void rejectBookingShouldReturnRejected() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", ownerId)
                        .param("approved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Подтверждение бронирования — должен вернуть 403 не владельцу вещи")
    void approveShouldReturn403WhenNotOwner() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long strangerId = createUser("Stranger");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", strangerId)
                        .param("approved", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Подтверждение бронирования — должен вернуть 404 для несуществующей брони")
    void approveShouldReturn404WhenBookingNotFound() throws Exception {
        Long ownerId = createUser("Owner");

        mockMvc.perform(patch("/bookings/999999")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("approved", "true"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Подтверждение бронирования — должен вернуть 400 при повторном решении")
    void approveShouldReturn400WhenAlreadyResolved() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        approve(ownerId, bookingId, true);

        mockMvc.perform(patch("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", ownerId)
                        .param("approved", "true"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Поиск бронирования — должен вернуть 200 бронирующему")
    void findBookingByIdShouldReturn200ForBooker() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));
    }

    @Test
    @DisplayName("Поиск бронирования — должен вернуть 200 владельцу вещи")
    void findBookingByIdShouldReturn200ForOwner() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(bookingId));
    }

    @Test
    @DisplayName("Поиск бронирования — должен вернуть 404 постороннему пользователю")
    void findBookingByIdShouldReturn404ForStranger() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long strangerId = createUser("Stranger");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings/" + bookingId)
                        .header("X-Sharer-User-Id", strangerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Поиск бронирования — должен вернуть 404 для несуществующей брони")
    void findBookingByIdShouldReturn404WhenNotFound() throws Exception {
        Long bookerId = createUser("Booker");

        mockMvc.perform(get("/bookings/999999")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=ALL")
    void findAllByBookerShouldReturnAll() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=CURRENT")
    void findAllByBookerShouldReturnCurrent() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), BookingStatus.APPROVED);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "CURRENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=PAST")
    void findAllByBookerShouldReturnPast() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "PAST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=FUTURE")
    void findAllByBookerShouldReturnFuture() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "FUTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=WAITING")
    void findAllByBookerShouldReturnWaiting() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 200 для state=REJECTED")
    void findAllByBookerShouldReturnRejected() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        approve(ownerId, bookingId, false);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований — должен вернуть 200 для state по умолчанию")
    void findAllByBookerShouldReturnAllByDefault() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 400 для неизвестного state")
    void findAllByBookerShouldReturn400ForUnknownState() throws Exception {
        Long bookerId = createUser("Booker");

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId)
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть 404 для несуществующего пользователя")
    void findAllByBookerShouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список бронирований пользователя — должен вернуть сортировку по убыванию даты")
    void findAllByBookerShouldReturnSortedDesc() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long item1 = createItem(ownerId, "Drill", true);
        Long item2 = createItem(ownerId, "Saw", true);
        createBooking(bookerId, item1, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        createBooking(bookerId, item2, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(6));

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item.id").value(item2))
                .andExpect(jsonPath("$[1].item.id").value(item1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=ALL")
    void findAllByOwnerShouldReturnAll() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=CURRENT")
    void findAllByOwnerShouldReturnCurrent() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1), BookingStatus.APPROVED);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "CURRENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=PAST")
    void findAllByOwnerShouldReturnPast() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "PAST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=FUTURE")
    void findAllByOwnerShouldReturnFuture() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        approve(ownerId, bookingId, true);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "FUTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=WAITING")
    void findAllByOwnerShouldReturnWaiting() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 200 для state=REJECTED")
    void findAllByOwnerShouldReturnRejected() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        Long bookingId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        approve(ownerId, bookingId, false);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", ownerId)
                        .param("state", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Список бронирований владельца — должен вернуть 404 для несуществующего пользователя")
    void findAllByOwnerShouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Вещь владельца — должен вернуть lastBooking и nextBooking")
    void ownerSeesLastAndNextBooking() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);

        Long lastId = seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);
        Long nextId = createBooking(bookerId, itemId,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));
        approve(ownerId, nextId, true);

        mockMvc.perform(get("/items/" + itemId)
                        .header("X-Sharer-User-Id", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking.id").value(lastId))
                .andExpect(jsonPath("$.nextBooking.id").value(nextId));
    }

    @Test
    @DisplayName("Вещь для постороннего — lastBooking и nextBooking должны быть null")
    void strangerDoesNotSeeLastAndNextBooking() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(get("/items/" + itemId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastBooking").value(nullValue()))
                .andExpect(jsonPath("$.nextBooking").value(nullValue()));
    }

    @Test
    @DisplayName("Добавление отзыва — должен вернуть 200 после завершённой аренды")
    void addCommentShouldReturn200() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Great drill!", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.text").value("Great drill!"))
                .andExpect(jsonPath("$.authorName").value("Booker"))
                .andExpect(jsonPath("$.created").isNotEmpty());
    }

    @Test
    @DisplayName("Добавление отзыва — должен вернуть 400 без завершённой аренды")
    void addCommentShouldReturn400WhenNoCompletedRental() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        createBooking(bookerId, itemId, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Great drill!", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Добавление отзыва — должен вернуть 400 при пустом тексте")
    void addCommentShouldReturn400WhenTextBlank() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, " ", null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Добавление отзыва — должен вернуть 404 для несуществующего пользователя")
    void addCommentShouldReturn404WhenUserNotFound() throws Exception {
        Long ownerId = createUser("Owner");
        Long itemId = createItem(ownerId, "Drill", true);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header("X-Sharer-User-Id", 999999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Great drill!", null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Добавление отзыва — должен вернуть 404 для несуществующей вещи")
    void addCommentShouldReturn404WhenItemNotFound() throws Exception {
        Long bookerId = createUser("Booker");

        mockMvc.perform(post("/items/999999/comment")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Great drill!", null, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Отзыв — должен отображаться в списке комментариев вещи")
    void commentShouldAppearInGetItem() throws Exception {
        Long ownerId = createUser("Owner");
        Long bookerId = createUser("Booker");
        Long itemId = createItem(ownerId, "Drill", true);
        seedBooking(itemId, bookerId, LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1), BookingStatus.APPROVED);

        mockMvc.perform(post("/items/" + itemId + "/comment")
                        .header("X-Sharer-User-Id", bookerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CommentDto(null, "Great drill!", null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/items/" + itemId)
                        .header("X-Sharer-User-Id", bookerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments.length()").value(1))
                .andExpect(jsonPath("$.comments[0].text").value("Great drill!"))
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));
    }
}