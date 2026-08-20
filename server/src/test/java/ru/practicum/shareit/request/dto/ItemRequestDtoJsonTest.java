package ru.practicum.shareit.request.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeCreatedInCustomFormat() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(1L);
        dto.setDescription("Нужна дрель");
        dto.setRequestorId(2L);
        dto.setItems(List.of());
        dto.setCreated(LocalDateTime.of(2024, 5, 6, 12, 30, 45));

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"created\":\"2024-05-06T12:30:45\"");
        assertThat(json).contains("\"description\":\"Нужна дрель\"");
    }

    @Test
    void shouldDeserializeCreatedFromCustomFormat() throws Exception {
        String json = "{\"id\":1,\"description\":\"Нужна дрель\",\"requestorId\":2,"
                + "\"items\":[],\"created\":\"2024-05-06T12:30:45\"}";

        ItemRequestDto dto = objectMapper.readValue(json, ItemRequestDto.class);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
        assertThat(dto.getCreated()).isEqualTo(LocalDateTime.of(2024, 5, 6, 12, 30, 45));
    }

    @Test
    void shouldSerializeNestedItemsWithRequestId() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(1L);
        dto.setDescription("Нужна дрель");
        dto.setRequestorId(2L);
        dto.setItems(List.of(new ItemDto(10L, "Дрель", "Мощная", true, 3L, 1L)));
        dto.setCreated(LocalDateTime.of(2024, 5, 6, 12, 30, 45));

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"requestId\":1");
        assertThat(json).contains("\"items\":[{\"id\":10");
    }
}