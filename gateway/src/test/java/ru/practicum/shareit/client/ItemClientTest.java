package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ItemClientTest {

    private ItemClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new ItemClient("http://localhost:9090", new RestTemplateBuilder());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void createItemShouldSendUserIdHeaderAndBody() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.createItem(1L,
                new ItemDto(null, "Дрель", "Мощная", true, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void searchItemsShouldSendUserIdHeader() {
        server.expect(requestTo("http://localhost:9090/items/search?text=drill"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.searchItems(1L, "drill");

        server.verify();
    }

    @Test
    void addCommentShouldPostToCommentEndpoint() {
        server.expect(requestTo("http://localhost:9090/items/5/comment"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        client.addComment(2L, 5L, new CommentDto(null, "Отличная вещь", null, null));

        server.verify();
    }

    @Test
    void updateItemShouldSendUserIdHeaderAndBody() {
        server.expect(requestTo("http://localhost:9090/items/5"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.updateItem(1L, 5L,
                new ItemDto(null, "Шуруповерт", null, null, null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void getItemShouldSendUserIdHeader() {
        server.expect(requestTo("http://localhost:9090/items/5"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("{\"id\":5}", MediaType.APPLICATION_JSON));

        client.getItem(5L, 1L);

        server.verify();
    }

    @Test
    void getItemsByUserShouldSendUserIdHeader() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "3"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.getItemsByUser(3L);

        server.verify();
    }
}