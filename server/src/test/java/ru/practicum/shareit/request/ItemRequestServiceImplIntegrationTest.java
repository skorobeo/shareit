package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
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
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRequestRepository itemRequestRepository;
    @Autowired
    private UserRepository userRepository;

    private UserDto createUser(String email) {
        return userService.create(new UserDto(null, email.split("@")[0], email));
    }

    @Test
    void createShouldSetRequestorAndCreated() {
        UserDto requestor = createUser("req-owner@mail.com");

        ItemRequestDto dto = requestService.create(requestor.getId(),
                new ItemRequestCreateDto("Нужна дрель"));

        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
        assertThat(dto.getRequestorId()).isEqualTo(requestor.getId());
        assertThat(dto.getCreated()).isNotNull();
        assertThat(dto.getItems()).isEmpty();
    }

    @Test
    void createWithUnknownUserShouldThrow() {
        assertThatThrownBy(() -> requestService.create(999L,
                new ItemRequestCreateDto("Нужна дрель")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllByRequestorShouldReturnOwnRequestsNewestFirst() {
        // Создаём пользователя
        UserDto requestorDto = createUser("req-owner2@mail.com");
        User requestor = userRepository.findById(requestorDto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Создаём первый запрос с явной датой (более ранней)
        ItemRequest request1 = new ItemRequest();
        request1.setDescription("Нужна дрель");
        request1.setRequestor(requestor);
        request1.setCreated(LocalDateTime.now().minusMinutes(5));
        itemRequestRepository.save(request1);

        // Создаём второй запрос с более поздней датой
        ItemRequest request2 = new ItemRequest();
        request2.setDescription("Нужна пила");
        request2.setRequestor(requestor);
        request2.setCreated(LocalDateTime.now());
        itemRequestRepository.save(request2);

        // Вызываем сервис
        List<ItemRequestDto> requests = requestService.findAllByRequestor(requestorDto.getId());

        // Проверяем, что запросы отсортированы по created DESC (сначала новый)
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getDescription()).isEqualTo("Нужна пила");
        assertThat(requests.get(1).getDescription()).isEqualTo("Нужна дрель");
    }

    @Test
    void findAllByRequestorWithUnknownUserShouldThrow() {
        assertThatThrownBy(() -> requestService.findAllByRequestor(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllOthersShouldExcludeOwnRequests() {
        UserDto requestor = createUser("req-owner3@mail.com");
        UserDto viewer = createUser("req-viewer3@mail.com");
        requestService.create(requestor.getId(), new ItemRequestCreateDto("Нужна дрель"));

        List<ItemRequestDto> others = requestService.findAllOthers(viewer.getId());

        assertThat(others).hasSize(1);
        assertThat(others.get(0).getDescription()).isEqualTo("Нужна дрель");

        List<ItemRequestDto> own = requestService.findAllOthers(requestor.getId());
        assertThat(own).isEmpty();
    }

    @Test
    void findAllOthersWithUnknownUserShouldThrow() {
        assertThatThrownBy(() -> requestService.findAllOthers(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdShouldReturnRequestWithLinkedItems() {
        UserDto requestor = createUser("req-owner4@mail.com");
        UserDto owner = createUser("req-owner5@mail.com");
        Long requestId = requestService.create(requestor.getId(),
                new ItemRequestCreateDto("Нужна дрель")).getId();

        itemService.create(owner.getId(),
                new ItemDto(null, "Дрель", "Мощная дрель", true, null, requestId));

        ItemRequestDto found = requestService.findById(requestor.getId(), requestId);

        assertThat(found.getId()).isEqualTo(requestId);
        assertThat(found.getItems()).hasSize(1);
        assertThat(found.getItems().get(0).getName()).isEqualTo("Дрель");
        assertThat(found.getItems().get(0).getRequestId()).isEqualTo(requestId);
    }

    @Test
    void findByIdWithUnknownUserShouldThrow() {
        UserDto requestor = createUser("req-owner6@mail.com");
        Long requestId = requestService.create(requestor.getId(),
                new ItemRequestCreateDto("Нужна дрель")).getId();

        assertThatThrownBy(() -> requestService.findById(999L, requestId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdWithUnknownRequestShouldThrow() {
        UserDto requestor = createUser("req-owner7@mail.com");

        assertThatThrownBy(() -> requestService.findById(requestor.getId(), 999L))
                .isInstanceOf(NotFoundException.class);
    }
}