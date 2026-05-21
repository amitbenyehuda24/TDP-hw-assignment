package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks: status transitions, DONE lock, and dependency-blocker gate.
 */
class TicketLifecycleTest extends AbstractIntegrationTest {

    @MockitoBean
    TicketEscalationScheduler schedulerMock;

    private String token;
    private Long projectId;

    @BeforeEach
    void setup() throws Exception {
        token = registerAndLogin("dev1", "DEVELOPER");
        projectId = createProject(token);
    }

    @Test
    void validTransition_todoToInProgress_returns200() throws Exception {
        Long ticketId = createTicket(token, projectId, "LOW");

        mockMvc.perform(patch("/tickets/" + ticketId)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void invalidTransition_inProgressToTodo_returns400() throws Exception {
        Long ticketId = createTicket(token, projectId, "LOW");

        // advance to IN_PROGRESS first
        mockMvc.perform(patch("/tickets/" + ticketId)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
            .andExpect(status().isOk());

        // backward transition must be rejected
        mockMvc.perform(patch("/tickets/" + ticketId)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("status", "TODO"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateDoneTicket_returns400() throws Exception {
        Long ticketId = createTicket(token, projectId, "LOW");

        // bring ticket to DONE: TODO → IN_PROGRESS → IN_REVIEW → DONE
        for (String status : new String[]{"IN_PROGRESS", "IN_REVIEW", "DONE"}) {
            mockMvc.perform(patch("/tickets/" + ticketId)
                .header("Authorization", auth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
        }

        // any further update must be rejected
        mockMvc.perform(patch("/tickets/" + ticketId)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("title", "new title"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void markDoneWithUnresolvedBlocker_returns400() throws Exception {
        Long blocked = createTicket(token, projectId, "HIGH");
        Long blocker = createTicket(token, projectId, "LOW");

        // add dependency: blocked is blocked by blocker
        mockMvc.perform(post("/tickets/" + blocked + "/dependencies")
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("blocked_by", blocker))))
            .andExpect(status().isCreated());

        // advance blocked to IN_REVIEW so it can attempt DONE
        for (String status : new String[]{"IN_PROGRESS", "IN_REVIEW"}) {
            mockMvc.perform(patch("/tickets/" + blocked)
                .header("Authorization", auth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
        }

        // blocker is still TODO → marking blocked as DONE must fail
        mockMvc.perform(patch("/tickets/" + blocked)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void markDoneAfterBlockerResolved_returns200() throws Exception {
        Long blocked = createTicket(token, projectId, "HIGH");
        Long blocker = createTicket(token, projectId, "LOW");

        mockMvc.perform(post("/tickets/" + blocked + "/dependencies")
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("blocked_by", blocker))))
            .andExpect(status().isCreated());

        // resolve the blocker ticket all the way to DONE
        for (String status : new String[]{"IN_PROGRESS", "IN_REVIEW", "DONE"}) {
            mockMvc.perform(patch("/tickets/" + blocker)
                .header("Authorization", auth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
        }

        // advance blocked to IN_REVIEW
        for (String status : new String[]{"IN_PROGRESS", "IN_REVIEW"}) {
            mockMvc.perform(patch("/tickets/" + blocked)
                .header("Authorization", auth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
        }

        // blocker is now DONE → marking blocked as DONE must succeed
        mockMvc.perform(patch("/tickets/" + blocked)
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("status", "DONE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"));
    }
}
