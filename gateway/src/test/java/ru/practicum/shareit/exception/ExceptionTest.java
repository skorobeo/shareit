package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    void exceptionsShouldCarryMessage() {
        assertThat(new NotFoundException("nf").getMessage()).isEqualTo("nf");
        assertThat(new ConflictException("conf").getMessage()).isEqualTo("conf");
        assertThat(new ForbiddenException("forb").getMessage()).isEqualTo("forb");
        assertThat(new ValidationException("val").getMessage()).isEqualTo("val");
    }

    @Test
    void errorResponseShouldExposeFields() {
        ErrorResponse response = new ErrorResponse("Not Found", "message");

        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("message");
    }
}