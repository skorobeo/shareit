package ru.practicum.shareit.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = new User(1L, "John", "john@mail.com");
        userDto = new UserDto(1L, "John", "john@mail.com");
    }

    @Test
    void create_shouldSaveUser_whenEmailNotExists() {
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.create(userDto);

        assertThat(result).isEqualTo(userDto);
        verify(userRepository, times(1)).findByEmail(userDto.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void create_shouldThrowConflictException_whenEmailExists() {
        when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.create(userDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldUpdateUser_whenUserExistsAndEmailFree() {
        UserDto updateDto = new UserDto(null, "John Updated", "john.updated@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserDto result = userService.update(1L, updateDto);

        assertThat(result.getName()).isEqualTo("John Updated");
        assertThat(result.getEmail()).isEqualTo("john.updated@mail.com");
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByEmail(updateDto.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void update_shouldThrowConflictException_whenEmailTakenByOtherUser() {
        User otherUser = new User(2L, "Jane", "john.updated@mail.com");
        UserDto updateDto = new UserDto(null, "John Updated", "john.updated@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(updateDto.getEmail())).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> userService.update(1L, updateDto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже занят");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(999L, userDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void findById_shouldReturnUser_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDto result = userService.findById(1L);

        assertThat(result).isEqualTo(userDto);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void findById_shouldThrowNotFoundException_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(userDto);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void delete_shouldDeleteUser() {
        userService.delete(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }
}