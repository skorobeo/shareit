package ru.practicum.shareit.booking.dto;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.exception.ValidationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StateTest {

    @Test
    void fromNullShouldReturnAll() {
        assertThat(State.from(null)).isEqualTo(State.ALL);
    }

    @Test
    void fromBlankShouldReturnAll() {
        assertThat(State.from(" ")).isEqualTo(State.ALL);
    }

    @Test
    void fromValidValueShouldIgnoreCase() {
        assertThat(State.from("waiting")).isEqualTo(State.WAITING);
        assertThat(State.from("CURRENT")).isEqualTo(State.CURRENT);
        assertThat(State.from("past")).isEqualTo(State.PAST);
        assertThat(State.from("FUTURE")).isEqualTo(State.FUTURE);
        assertThat(State.from("rejected")).isEqualTo(State.REJECTED);
        assertThat(State.from("all")).isEqualTo(State.ALL);
    }

    @Test
    void fromInvalidValueShouldThrow() {
        assertThatThrownBy(() -> State.from("UNKNOWN"))
                .isInstanceOf(ValidationException.class);
    }
}