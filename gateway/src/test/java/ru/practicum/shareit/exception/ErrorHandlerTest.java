package ru.practicum.shareit.exception;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    @Test
    void handleNotFoundShouldReturnNotFound() {
        ErrorResponse response = handler.handleNotFound(new NotFoundException("nf"));

        assertThat(response.getError()).isEqualTo("Not Found");
        assertThat(response.getMessage()).isEqualTo("nf");
    }

    @Test
    void handleConflictShouldReturnConflict() {
        ErrorResponse response = handler.handleConflict(new ConflictException("conf"));

        assertThat(response.getError()).isEqualTo("Conflict");
        assertThat(response.getMessage()).isEqualTo("conf");
    }

    @Test
    void handleForbiddenShouldReturnForbidden() {
        ErrorResponse response = handler.handleForbidden(new ForbiddenException("forb"));

        assertThat(response.getError()).isEqualTo("Forbidden");
        assertThat(response.getMessage()).isEqualTo("forb");
    }

    @Test
    void handleRuntimeShouldReturnInternalServerError() {
        ErrorResponse response = handler.handleRuntime(new RuntimeException("boom"));

        assertThat(response.getError()).isEqualTo("Internal Server Error");
        assertThat(response.getMessage()).isEqualTo("boom");
    }

    @Test
    void handleValidationForBeanValidationShouldJoinFieldErrors() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("dto", "name", "не может быть пустым"),
                        new FieldError("dto", "email", "некорректный")));

        ErrorResponse response = handler.handleValidation(exception);

        assertThat(response.getError()).isEqualTo("Validation Error");
        assertThat(response.getMessage()).isEqualTo("name: не может быть пустым; email: некорректный");
    }

    @Test
    void handleValidationForCustomExceptionShouldReturnMessage() {
        ErrorResponse response = handler.handleValidation(new ValidationException("val"));

        assertThat(response.getError()).isEqualTo("Validation Error");
        assertThat(response.getMessage()).isEqualTo("val");
    }
}