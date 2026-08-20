package ru.practicum.shareit.request;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemRequestServiceImplIntegrationTest {

    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;

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
    void findAllByRequestorShouldReturnOwnRequestsNewestFirst() throws InterruptedException {
        UserDto requestor = createUser("req-owner2@mail.com");
        requestService.create(requestor.getId(), new ItemRequestCreateDto("Нужна дрель"));
        Thread.sleep(2);
        requestService.create(requestor.getId(), new ItemRequestCreateDto("Нужна пила"));

        List<ItemRequestDto> requests = requestService.findAllByRequestor(requestor.getId());

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