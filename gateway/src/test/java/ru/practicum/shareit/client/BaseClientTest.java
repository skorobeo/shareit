package ru.practicum.shareit.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BaseClientTest {

    private BaseClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new BaseClient("http://localhost:9090", new RestTemplateBuilder()) {};
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getWithoutHeadersShouldSetDefaultContentType() {
        server.expect(requestTo("http://localhost:9090/items"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.get("/items");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void putWithBodyShouldSendRequest() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.put("/users/1", "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void putWithHeadersShouldSendRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Sharer-User-Id", "1");

        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.put("/users/1", headers, "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void patchWithoutHeadersShouldSendRequest() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.patch("/users/1", "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void deleteWithHeadersShouldSendRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Sharer-User-Id", "1");

        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-Sharer-User-Id", "1"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.delete("/users/1", headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void postWithoutHeadersShouldSendRequest() {
        server.expect(requestTo("http://localhost:9090/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ok", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.post("/users", "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }

    @Test
    void deleteWithoutHeadersShouldSendRequest() {
        server.expect(requestTo("http://localhost:9090/users/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        ResponseEntity<String> response = client.delete("/users/1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        server.verify();
    }
}