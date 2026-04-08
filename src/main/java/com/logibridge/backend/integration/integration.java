//package com.logibridge.backend.auth;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.logibridge.backend.auth.dto.*;
//import com.logibridge.backend.auth.enums.RoleName;
//import com.logibridge.backend.auth.repository.UserRepository;
//import com.logibridge.backend.order.dto.CreateOrderRequest;
//import com.logibridge.backend.order.dto.UpdateOrderStatusRequest;
//import com.logibridge.backend.order.enums.OrderStatus;
//import com.logibridge.backend.order.repository.OrderRepository;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.MvcResult;
//import org.springframework.transaction.annotation.Transactional;
//
//import static org.hamcrest.Matchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@ActiveProfiles("test")
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//class AuthIntegrationTest {
//
//    @Autowired MockMvc mockMvc;
//    @Autowired ObjectMapper objectMapper;
//    @Autowired UserRepository userRepository;
//    @Autowired OrderRepository orderRepository;
//
//    // Shared state across ordered tests
//    private static String companyAccessToken;
//    private static String deliveryAccessToken;
//    private static String createdOrderNumber;
//
//    // ──────────────────────────────────────────────
//    // 1. REGISTER
//    // ──────────────────────────────────────────────
//
//    @Test
//    @org.junit.jupiter.api.Order(1)
//    void register_company_user_returns_201_with_tokens() throws Exception {
//
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("Ahmed")
//                .lastName("Hassan")
//                .email("company@test.com")
//                .password("Company1pass")
//                .role(RoleName.ROLE_COMPANY)
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(2)
//    void register_delivery_user_returns_200_with_tokens() throws Exception {
//
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("Omar")
//                .lastName("Delivery")
//                .email("delivery@test.com")
//                .password("Delivery1pass")
//                .role(RoleName.ROLE_DELIVERY)
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(3)
//    void register_duplicate_email_returns_409() throws Exception {
//
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("Ahmed")
//                .lastName("Hassan")
//                .email("company@test.com")   // duplicate
//                .password("Company1pass")
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isConflict());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(4)
//    void register_invalid_password_returns_400() throws Exception {
//
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("Test")
//                .lastName("User")
//                .email("bad@test.com")
//                .password("weak")   // violates pattern + min length
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // ──────────────────────────────────────────────
//    // 2. LOGIN
//    // ──────────────────────────────────────────────
//
//    @Test
//    @org.junit.jupiter.api.Order(5)
//    void login_company_user_returns_200_and_stores_token() throws Exception {
//
//        LoginRequest request = new LoginRequest("company@test.com", "Company1pass");
//
//        MvcResult result = mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andReturn();
//
//        AuthResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(), AuthResponse.class);
//
//        companyAccessToken = response.getAccessToken();
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(6)
//    void login_delivery_user_returns_200_and_stores_token() throws Exception {
//
//        LoginRequest request = new LoginRequest("delivery@test.com", "Delivery1pass");
//
//        MvcResult result = mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").isNotEmpty())
//                .andReturn();
//
//        AuthResponse response = objectMapper.readValue(
//                result.getResponse().getContentAsString(), AuthResponse.class);
//
//        deliveryAccessToken = response.getAccessToken();
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(7)
//    void login_wrong_password_returns_401() throws Exception {
//
//        LoginRequest request = new LoginRequest("company@test.com", "WrongPass99");
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(8)
//    void login_unknown_email_returns_401() throws Exception {
//
//        LoginRequest request = new LoginRequest("ghost@test.com", "Whatever1pass");
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // ──────────────────────────────────────────────
//    // 3. CREATE ORDER (ROLE_COMPANY)
//    // ──────────────────────────────────────────────
//
//    @Test
//    @org.junit.jupiter.api.Order(9)
//    void create_order_with_company_jwt_returns_201() throws Exception {
//
//        CreateOrderRequest request = CreateOrderRequest.builder()
//                .recipientName("Mohamed Ali")
//                .recipientPhone("01012345678")
//                .deliveryAddress("123 Cairo St, Cairo")
//                .build();
//
//        MvcResult result = mockMvc.perform(post("/api/orders")
//                        .header("Authorization", "Bearer " + companyAccessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
//                .andExpect(jsonPath("$.status").value("PENDING"))
//                .andReturn();
//
//        OrderResponse orderResponse = objectMapper.readValue(
//                result.getResponse().getContentAsString(), OrderResponse.class);
//
//        createdOrderNumber = orderResponse.getOrderNumber();
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(10)
//    void create_order_without_jwt_returns_401() throws Exception {
//
//        CreateOrderRequest request = CreateOrderRequest.builder()
//                .recipientName("Test")
//                .recipientPhone("01099999999")
//                .deliveryAddress("Some address")
//                .build();
//
//        mockMvc.perform(post("/api/orders")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(11)
//    void create_order_with_delivery_jwt_returns_403() throws Exception {
//
//        CreateOrderRequest request = CreateOrderRequest.builder()
//                .recipientName("Test")
//                .recipientPhone("01099999999")
//                .deliveryAddress("Some address")
//                .build();
//
//        mockMvc.perform(post("/api/orders")
//                        .header("Authorization", "Bearer " + deliveryAccessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
//
//    // ──────────────────────────────────────────────
//    // 4. UPDATE ORDER STATUS (ROLE_DELIVERY)
//    // ──────────────────────────────────────────────
//
//    @Test
//    @org.junit.jupiter.api.Order(12)
//    void update_order_status_to_in_progress_with_delivery_jwt_returns_200() throws Exception {
//
//        Assumptions.assumeTrue(createdOrderNumber != null,
//                "Skipping — order creation test did not run or failed");
//
//        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
//                .status(OrderStatus.IN_PROGRESS)
//                .location("Cairo Warehouse")
//                .build();
//
//        mockMvc.perform(put("/api/orders/" + createdOrderNumber + "/status")
//                        .header("Authorization", "Bearer " + deliveryAccessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(13)
//    void update_order_status_with_company_jwt_returns_403() throws Exception {
//
//        Assumptions.assumeTrue(createdOrderNumber != null,
//                "Skipping — order creation test did not run or failed");
//
//        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
//                .status(OrderStatus.IN_PROGRESS)
//                .location("Cairo")
//                .build();
//
//        mockMvc.perform(put("/api/orders/" + createdOrderNumber + "/status")
//                        .header("Authorization", "Bearer " + companyAccessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @org.junit.jupiter.api.Order(14)
//    void update_order_status_on_nonexistent_order_returns_404() throws Exception {
//
//        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
//                .status(OrderStatus.IN_PROGRESS)
//                .location("Cairo")
//                .build();
//
//        mockMvc.perform(put("/api/orders/ORD-NONEXISTENT/status")
//                        .header("Authorization", "Bearer " + deliveryAccessToken)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isNotFound());
//    }
//}