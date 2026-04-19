package com.logibridge.backend.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.order.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl() {
        return "http://localhost:" + port;
    }



    private void registerUser(String email, String password, String role) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", password,
                "role", role
        );

        restTemplate.postForEntity(
                baseUrl() + "/api/auth/register",
                request,
                String.class
        );
    }

    private String loginAndGetToken(String email, String password) throws Exception {

        Map<String, String> request = Map.of(
                "email", email,
                "password", password
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/auth/login",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> body =
                objectMapper.readValue(response.getBody(), Map.class);

        return (String) ((Map<?, ?>) body.get("data")).get("accessToken");
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String extractOrderNumber(ResponseEntity<String> response) throws Exception {
        Map<String, Object> body =
                objectMapper.readValue(response.getBody(), Map.class);

        return (String) ((Map<?, ?>) body.get("data")).get("orderNumber");
    }



    @Test
    void fullFlow_create_accept_idempotent() throws Exception {


        registerUser("company@test.com", "123456", "COMPANY");
        registerUser("delivery@test.com", "123456", "DELIVERY");


        String companyToken = loginAndGetToken("company@test.com", "123456");


        CreateOrderRequest request = new CreateOrderRequest();
        request.setPickupAddress("A");
        request.setDeliveryAddress("B");

        HttpEntity<CreateOrderRequest> entity =
                new HttpEntity<>(request, authHeaders(companyToken));

        ResponseEntity<String> createResponse =
                restTemplate.postForEntity(
                        baseUrl() + "/api/orders",
                        entity,
                        String.class
                );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String orderNumber = extractOrderNumber(createResponse);
        assertThat(orderNumber).isNotNull();


        String deliveryToken = loginAndGetToken("delivery@test.com", "123456");

        String idempotencyKey = "test-key-123";

        HttpHeaders headers = authHeaders(deliveryToken);
        headers.add("Idempotency-Key", idempotencyKey);

        HttpEntity<Void> acceptEntity = new HttpEntity<>(headers);


        ResponseEntity<String> acceptResponse1 =
                restTemplate.postForEntity(
                        baseUrl() + "/api/delivery/orders/" + orderNumber + "/accept",
                        acceptEntity,
                        String.class
                );

        assertThat(acceptResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);


        ResponseEntity<String> acceptResponse2 =
                restTemplate.postForEntity(
                        baseUrl() + "/api/delivery/orders/" + orderNumber + "/accept",
                        acceptEntity,
                        String.class
                );

        assertThat(acceptResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);


        assertThat(acceptResponse1.getBody()).isEqualTo(acceptResponse2.getBody());
    }
}