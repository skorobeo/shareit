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
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RequestClientTest {

    private RequestClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new RequestClient("http://localhost:9090", new RestTemplateBuilder());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void createRequestShouldPostWithUserIdHeader() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.createRequest(2L,
                new ItemRequestCreateDto("Нужна дрель"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void getOwnRequestsShouldGetWithUserIdHeader() {
        server.expect(requestTo("http://localhost:9090/requests"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.getOwnRequests(2L);

        server.verify();
    }

    @Test
    void getAllRequestsShouldGetAllPath() {
        server.expect(requestTo("http://localhost:9090/requests/all"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.getAllRequests(1L);

        server.verify();
    }

    @Test
    void getRequestShouldGetRequestPath() {
        server.expect(requestTo("http://localhost:9090/requests/1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sharer-User-Id", "2"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        client.getRequest(2L, 1L);

        server.verify();
    }
}