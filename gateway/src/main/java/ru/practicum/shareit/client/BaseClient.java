package ru.practicum.shareit.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class BaseClient {
    private final RestTemplate restTemplate;
    private final String serverUrl;

    public BaseClient(@Value("${shareit-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        this.serverUrl = serverUrl;
        this.restTemplate = builder
                .requestFactory(() -> new JdkClientHttpRequestFactory())
                .build();
    }

    protected ResponseEntity<String> get(String path) {
        return get(path, null);
    }

    protected ResponseEntity<String> get(String path, HttpHeaders headers) {
        return exchange(path, HttpMethod.GET, headers, null);
    }

    protected ResponseEntity<String> post(String path, Object body) {
        return post(path, null, body);
    }

    protected ResponseEntity<String> post(String path, HttpHeaders headers, Object body) {
        return exchange(path, HttpMethod.POST, headers, body);
    }

    protected ResponseEntity<String> put(String path, Object body) {
        return put(path, null, body);
    }

    protected ResponseEntity<String> put(String path, HttpHeaders headers, Object body) {
        return exchange(path, HttpMethod.PUT, headers, body);
    }

    protected ResponseEntity<String> patch(String path, Object body) {
        return patch(path, null, body);
    }

    protected ResponseEntity<String> patch(String path, HttpHeaders headers, Object body) {
        return exchange(path, HttpMethod.PATCH, headers, body);
    }

    protected ResponseEntity<String> delete(String path) {
        return delete(path, null);
    }

    protected ResponseEntity<String> delete(String path, HttpHeaders headers) {
        return exchange(path, HttpMethod.DELETE, headers, null);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, HttpHeaders headers, Object body) {
        HttpHeaders finalHeaders = new HttpHeaders();
        if (headers != null) {
            finalHeaders.putAll(headers);
        }
        if (!finalHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
            finalHeaders.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, finalHeaders);
        try {
            return restTemplate.exchange(serverUrl + path, method, entity, String.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }
}