package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks: soft delete hides resources, ADMIN gate on deleted/restore, restore brings them back.
 */
class SoftDeleteTest extends AbstractIntegrationTest {

    @MockitoBean
    TicketEscalationScheduler schedulerMock;

    private String adminToken;
    private String devToken;
    private Long projectId;

    @BeforeEach
    void setup() throws Exception {
        adminToken = registerAndLogin("admin1", "ADMIN");
        devToken   = registerAndLogin("dev1",   "DEVELOPER");
        projectId  = createProject(adminToken);
    }

    // --- Ticket soft delete & restore ---

    @Test
    void deleteTicket_thenGet_returns404() throws Exception {
        Long ticketId = createTicket(adminToken, projectId, "LOW");

        mockMvc.perform(delete("/tickets/" + ticketId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/tickets/" + ticketId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void nonAdmin_getDeletedTickets_returns403() throws Exception {
        mockMvc.perform(get("/tickets/deleted")
            .param("projectId", projectId.toString())
            .header("Authorization", auth(devToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_getDeletedTickets_returnsDeletedTicket() throws Exception {
        Long ticketId = createTicket(adminToken, projectId, "LOW");

        mockMvc.perform(delete("/tickets/" + ticketId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/tickets/deleted")
            .param("projectId", projectId.toString())
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ticketId));
    }

    @Test
    void admin_restoreTicket_thenGet_returns200() throws Exception {
        Long ticketId = createTicket(adminToken, projectId, "LOW");

        mockMvc.perform(delete("/tickets/" + ticketId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/tickets/" + ticketId + "/restore")
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ticketId));

        mockMvc.perform(get("/tickets/" + ticketId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk());
    }

    // --- Project soft delete & restore ---

    @Test
    void deleteProject_thenGet_returns404() throws Exception {
        mockMvc.perform(delete("/projects/" + projectId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/" + projectId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNotFound());
    }

    @Test
    void nonAdmin_getDeletedProjects_returns403() throws Exception {
        mockMvc.perform(get("/projects/deleted")
            .header("Authorization", auth(devToken)))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_getDeletedProjects_returnsDeletedProject() throws Exception {
        mockMvc.perform(delete("/projects/" + projectId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/deleted")
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(projectId));
    }

    @Test
    void admin_restoreProject_thenGet_returns200() throws Exception {
        mockMvc.perform(delete("/projects/" + projectId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/projects/" + projectId + "/restore")
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(projectId));

        mockMvc.perform(get("/projects/" + projectId)
            .header("Authorization", auth(adminToken)))
            .andExpect(status().isOk());
    }
}
