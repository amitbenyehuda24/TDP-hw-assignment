package com.att.tdp.issueflow;

import com.att.tdp.issueflow.ticket.TicketEscalationScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Checks: auto-assign picks least-loaded dev, tiebreak by lowest ID,
 * null assignee when no devs exist, and workload endpoint counts.
 */
class AutoAssignmentTest extends AbstractIntegrationTest {

    @MockitoBean
    TicketEscalationScheduler schedulerMock;

    @Test
    void autoAssign_singleDeveloper_getsAssigned() throws Exception {
        String devToken = registerAndLogin("dev1", "DEVELOPER");
        Long devId = getUserId(devToken);
        Long projectId = createProject(devToken);

        // createTicket sends no assignee_id → auto-assign kicks in
        String resp = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/tickets")
            .header("Authorization", auth(devToken))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of(
                "title", "T1", "priority", "LOW", "type", "BUG", "project_id", projectId
            ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long assignedId = objectMapper.readTree(resp).get("assignee_id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(devId, assignedId);
    }

    @Test
    void autoAssign_tiebreak_assignsLowestId() throws Exception {
        String dev1Token = registerAndLogin("dev1", "DEVELOPER");
        registerAndLogin("dev2", "DEVELOPER"); // must exist to create a tie
        Long dev1Id = getUserId(dev1Token);
        Long projectId = createProject(dev1Token);

        // both devs have 0 tickets — dev1 (lower ID) should win
        String resp = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/tickets")
            .header("Authorization", auth(dev1Token))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of(
                "title", "T1", "priority", "LOW", "type", "BUG", "project_id", projectId
            ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        Long assignedId = objectMapper.readTree(resp).get("assignee_id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals(dev1Id, assignedId);
    }

    @Test
    void autoAssign_noDevelopers_assigneeIsNull() throws Exception {
        String adminToken = registerAndLogin("admin1", "ADMIN");
        Long projectId = createProject(adminToken);

        // no DEVELOPER users registered → assignee_id must be null
        String resp = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
            .post("/tickets")
            .header("Authorization", auth(adminToken))
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of(
                "title", "T1", "priority", "LOW", "type", "BUG", "project_id", projectId
            ))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertTrue(
            objectMapper.readTree(resp).get("assignee_id").isNull());
    }

    @Test
    void workload_returnsDevsSortedByOpenTicketCount() throws Exception {
        String dev1Token = registerAndLogin("dev1", "DEVELOPER");
        String dev2Token = registerAndLogin("dev2", "DEVELOPER");
        Long dev1Id = getUserId(dev1Token);
        Long dev2Id = getUserId(dev2Token);
        Long projectId = createProject(dev1Token);

        // Ticket 1 → dev1 (both at 0, dev1 wins tiebreak)
        // Ticket 2 → dev2 (dev1 at 1, dev2 at 0)
        // Ticket 3 → dev1 (both at 1, dev1 wins tiebreak)
        createTicket(dev1Token, projectId, "LOW");
        createTicket(dev1Token, projectId, "LOW");
        createTicket(dev1Token, projectId, "LOW");

        // Expected order: dev2 (1 ticket) before dev1 (2 tickets)
        mockMvc.perform(get("/projects/" + projectId + "/workload")
            .header("Authorization", auth(dev1Token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].user_id").value(dev2Id))
            .andExpect(jsonPath("$[0].open_ticket_count").value(1))
            .andExpect(jsonPath("$[1].user_id").value(dev1Id))
            .andExpect(jsonPath("$[1].open_ticket_count").value(2));
    }
}
