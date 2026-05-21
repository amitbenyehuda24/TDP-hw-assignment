package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Calls the scheduler directly to verify priority escalation rules and DONE-ticket exclusion.
 * Does NOT mock the scheduler — the real bean is injected and invoked.
 */
class EscalationSchedulerTest extends AbstractIntegrationTest {

    @Autowired
    TicketEscalationScheduler scheduler;

    private String token;
    private Long projectId;

    @BeforeEach
    void setup() throws Exception {
        token = registerAndLogin("dev1", "DEVELOPER");
        projectId = createProject(token);
    }

    /** Creates a ticket with a past due date so the scheduler picks it up. */
    private Long createOverdueTicket(String priority) throws Exception {
        String resp = mockMvc.perform(post("/tickets")
            .header("Authorization", auth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "title", "Overdue ticket",
                "priority", priority,
                "type", "BUG",
                "project_id", projectId,
                "due_date", "2020-01-01T00:00:00Z"
            ))))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("id").asLong();
    }

    @Test
    void escalate_lowPriority_bumpsToMedium() throws Exception {
        Long id = createOverdueTicket("LOW");

        scheduler.escalateOverdueTickets();

        mockMvc.perform(get("/tickets/" + id)
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("MEDIUM"))
            .andExpect(jsonPath("$.is_overdue").value(false));
    }

    @Test
    void escalate_mediumPriority_bumpsToHigh() throws Exception {
        Long id = createOverdueTicket("MEDIUM");

        scheduler.escalateOverdueTickets();

        mockMvc.perform(get("/tickets/" + id)
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.is_overdue").value(false));
    }

    @Test
    void escalate_highPriority_bumpsToCriticalAndSetsOverdue() throws Exception {
        Long id = createOverdueTicket("HIGH");

        scheduler.escalateOverdueTickets();

        mockMvc.perform(get("/tickets/" + id)
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("CRITICAL"))
            .andExpect(jsonPath("$.is_overdue").value(true));
    }

    @Test
    void escalate_criticalPriority_setsOverdueWithoutChangingPriority() throws Exception {
        Long id = createOverdueTicket("CRITICAL");

        scheduler.escalateOverdueTickets();

        mockMvc.perform(get("/tickets/" + id)
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("CRITICAL"))
            .andExpect(jsonPath("$.is_overdue").value(true));
    }

    @Test
    void escalate_doneTicket_notEscalated() throws Exception {
        Long id = createOverdueTicket("LOW");

        // advance to DONE before the scheduler runs
        for (String status : new String[]{"IN_PROGRESS", "IN_REVIEW", "DONE"}) {
            mockMvc.perform(patch("/tickets/" + id)
                .header("Authorization", auth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk());
        }

        scheduler.escalateOverdueTickets();

        mockMvc.perform(get("/tickets/" + id)
            .header("Authorization", auth(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.priority").value("LOW"))
            .andExpect(jsonPath("$.is_overdue").value(false));
    }
}
