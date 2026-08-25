package ru.practicum.shareit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void jsonCustomizerShouldSerializeLocalDateTimeAsString() throws Exception {
        JacksonConfig config = new JacksonConfig();
        Jackson2ObjectMapperBuilderCustomizer customizer = config.jsonCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        ObjectMapper mapper = builder.build();

        String json = mapper.writeValueAsString(
                Map.of("created", LocalDateTime.of(2024, 5, 6, 12, 30, 45)));

        assertThat(json).contains("\"created\":\"2024-05-06T12:30:45\"");
    }
}