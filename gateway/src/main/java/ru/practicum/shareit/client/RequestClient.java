package ru.practicum.shareit.client;
import ru.practicum.shareit.Constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.request.dto.ItemRequestCreateDto;

@Service
public class RequestClient extends BaseClient {
    public RequestClient(@Value("${shareit-server.url}") String serverUrl,
                         RestTemplateBuilder builder) {
        super(serverUrl, builder);
    }

    private HttpHeaders headersWithUserId(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(Constants.USER_ID_HEADER, String.valueOf(userId));
        return headers;
    }

    public ResponseEntity<String> createRequest(long userId, ItemRequestCreateDto requestDto) {
        return post("/requests", headersWithUserId(userId), requestDto);
    }

    public ResponseEntity<String> getOwnRequests(long userId) {
        return get("/requests", headersWithUserId(userId));
    }

    public ResponseEntity<String> getAllRequests(long userId) {
        return get("/requests/all", headersWithUserId(userId));
    }

    public ResponseEntity<String> getRequest(long userId, long requestId) {
        return get("/requests/" + requestId, headersWithUserId(userId));
    }
}