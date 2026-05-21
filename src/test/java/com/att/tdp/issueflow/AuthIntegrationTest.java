package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks: login, bad-password rejection, /auth/me, logout token invalidation.
 */
class AuthIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    TicketEscalationScheduler schedulerMock;

    @Test
    void login_validCredentials_returnsToken() throws Exception {
        registerAndLogin("admin1", "ADMIN");

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("username", "admin1", "password", "pass123"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        registerAndLogin("user1", "DEVELOPER");

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                Map.of("username", "user1", "password", "wrongpass"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/projects"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        String token = registerAndLogin("admin1", "ADMIN");

        mockMvc.perform(get("/auth/me")
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin1"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void logout_thenSameToken_returns401() throws Exception {
        String token = registerAndLogin("admin1", "ADMIN");

        mockMvc.perform(post("/auth/logout")
            .header("Authorization", auth(token)))
            .andExpect(status().isNoContent());

        // same token must now be rejected
        mockMvc.perform(get("/auth/me")
            .header("Authorization", auth(token)))
            .andExpect(status().isUnauthorized());
    }
}
