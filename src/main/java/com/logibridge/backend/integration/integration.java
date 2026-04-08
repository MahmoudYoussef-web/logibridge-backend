//package com.logibridge.backend.integration;
//
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.MediaType;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//class AuthIntegrationTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    static String accessToken;
//    static String refreshToken;
//
//    // =============================
//    // REGISTER
//    // =============================
//    @Test
//    @Order(1)
//    void register_success() throws Exception {
//
//        String request = """
//        {
//          "firstName": "Test",
//          "lastName": "User",
//          "email": "test@test.com",
//          "password": "Password123",
//          "phoneNumber": "01000000000"
//        }
//        """;
//
//        MvcResult result = mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.refreshToken").exists())
//                .andReturn();
//
//        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
//
//        accessToken = json.get("accessToken").asText();
//        refreshToken = json.get("refreshToken").asText();
//    }
//
//    // =============================
//    // LOGIN
//    // =============================
//    @Test
//    @Order(2)
//    void login_success() throws Exception {
//
//        String request = """
//        {
//          "email": "test@test.com",
//          "password": "Password123"
//        }
//        """;
//
//        MvcResult result = mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.refreshToken").exists())
//                .andReturn();
//
//        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
//
//        accessToken = json.get("accessToken").asText();
//        refreshToken = json.get("refreshToken").asText();
//    }
//
//    // =============================
//    // LOGIN FAIL
//    // =============================
//    @Test
//    @Order(3)
//    void login_invalid_password() throws Exception {
//
//        String request = """
//        {
//          "email": "test@test.com",
//          "password": "WrongPassword123"
//        }
//        """;
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // =============================
//    // REFRESH SUCCESS
//    // =============================
//    @Test
//    @Order(4)
//    void refresh_success() throws Exception {
//
//        String request = """
//        {
//          "refreshToken": "%s"
//        }
//        """.formatted(refreshToken);
//
//        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.accessToken").exists())
//                .andExpect(jsonPath("$.refreshToken").exists())
//                .andReturn();
//
//        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
//
//        refreshToken = json.get("refreshToken").asText(); // rotation
//    }
//
//    // =============================
//    // REFRESH REUSE (ATTACK)
//    // =============================
//    @Test
//    @Order(5)
//    void refresh_reuse_should_fail() throws Exception {
//
//        String request = """
//        {
//          "refreshToken": "%s"
//        }
//        """.formatted(refreshToken);
//
//        // First use → OK
//        mockMvc.perform(post("/api/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isOk());
//
//        // Second use → FAIL
//        mockMvc.perform(post("/api/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // =============================
//    // LOGOUT
//    // =============================
//    @Test
//    @Order(6)
//    void logout_success() throws Exception {
//
//        String request = """
//        {
//          "refreshToken": "%s"
//        }
//        """.formatted(refreshToken);
//
//        mockMvc.perform(post("/api/auth/logout")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isNoContent());
//    }
//
//    // =============================
//    // REFRESH AFTER LOGOUT
//    // =============================
//    @Test
//    @Order(7)
//    void refresh_after_logout_should_fail() throws Exception {
//
//        String request = """
//        {
//          "refreshToken": "%s"
//        }
//        """.formatted(refreshToken);
//
//        mockMvc.perform(post("/api/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isUnauthorized());
//    }
//
//    // =============================
//    // LOGOUT ALL
//    // =============================
//    @Test
//    @Order(8)
//    void logout_all_success() throws Exception {
//
//        mockMvc.perform(post("/api/auth/logout-all")
//                        .header("Authorization", "Bearer " + accessToken))
//                .andExpect(status().isOk());
//    }
//
//    // =============================
//    // REFRESH AFTER LOGOUT ALL
//    // =============================
//    @Test
//    @Order(9)
//    void refresh_after_logout_all_should_fail() throws Exception {
//
//        String request = """
//        {
//          "refreshToken": "%s"
//        }
//        """.formatted(refreshToken);
//
//        mockMvc.perform(post("/api/auth/refresh")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(request))
//                .andExpect(status().isUnauthorized());
//    }
//}