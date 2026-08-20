package ru.practicum.shareit.item.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemDtoJsonTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeRequestIdAndOwnerId() throws Exception {
        ItemDto dto = new ItemDto(1L, "Дрель", "Мощная", true, 2L, 7L);

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"requestId\":7");
        assertThat(json).contains("\"ownerId\":2");
        assertThat(json).contains("\"available\":true");
    }

    @Test
    void shouldDeserializeWithOptionalRequestId() throws Exception {
        String json = "{\"name\":\"Дрель\",\"description\":\"Мощная\",\"available\":true,"
                + "\"requestId\":7}";

        ItemDto dto = objectMapper.readValue(json, ItemDto.class);

        assertThat(dto.getName()).isEqualTo("Дрель");
        assertThat(dto.getAvailable()).isTrue();
        assertThat(dto.getRequestId()).isEqualTo(7L);
        assertThat(dto.getId()).isNull();
        assertThat(dto.getOwnerId()).isNull();
    }
}