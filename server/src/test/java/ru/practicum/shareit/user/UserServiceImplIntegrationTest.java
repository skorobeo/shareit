package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceImplIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void createShouldAssignIdAndPersist() {
        UserDto saved = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Ivan");
        assertThat(saved.getEmail()).isEqualTo("ivan@mail.com");
    }

    @Test
    void createWithDuplicateEmailShouldThrow() {
        userService.create(new UserDto(null, "Ivan", "dup@mail.com"));

        assertThatThrownBy(() -> userService.create(new UserDto(null, "Petr", "dup@mail.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateShouldChangeNameAndEmail() {
        UserDto created = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));

        UserDto updated = userService.update(created.getId(),
                new UserDto(null, "Petr", "petr@mail.com"));

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("Petr");
        assertThat(updated.getEmail()).isEqualTo("petr@mail.com");
    }

    @Test
    void updateNonexistentUserShouldThrow() {
        assertThatThrownBy(() -> userService.update(999L,
                new UserDto(null, "Petr", "petr@mail.com")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateWithEmailTakenByAnotherUserShouldThrow() {
        UserDto first = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));
        userService.create(new UserDto(null, "Petr", "petr@mail.com"));

        assertThatThrownBy(() -> userService.update(first.getId(),
                new UserDto(null, "Ivan", "petr@mail.com")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateWithBlankNameAndEmailShouldKeepOldValues() {
        UserDto created = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));

        UserDto updated = userService.update(created.getId(),
                new UserDto(null, "", " "));

        assertThat(updated.getName()).isEqualTo("Ivan");
        assertThat(updated.getEmail()).isEqualTo("ivan@mail.com");
    }

    @Test
    void findByIdShouldReturnUser() {
        UserDto created = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));

        UserDto found = userService.findById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Ivan");
    }

    @Test
    void findByIdNonexistentUserShouldThrow() {
        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllShouldReturnAllUsers() {
        userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));
        userService.create(new UserDto(null, "Petr", "petr@mail.com"));

        assertThat(userService.findAll()).hasSize(2);
    }

    @Test
    void deleteShouldRemoveUser() {
        UserDto created = userService.create(new UserDto(null, "Ivan", "ivan@mail.com"));

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.findById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}